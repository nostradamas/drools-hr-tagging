package com.northland.drools.tagging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.northland.drools.mapper.EmployeeTagMapper;
import com.northland.drools.entity.EmployeeTag;
import com.northland.drools.tagging.model.EnhancedEmployee;
import com.northland.drools.tagging.service.EmployeeTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工标签服务实现类
 */
@Slf4j
@Service
public class EmployeeTagServiceImpl extends ServiceImpl<EmployeeTagMapper, EmployeeTag> implements EmployeeTagService {

    @Value("${tagging.max-tags-per-employee:50}")
    private int maxTagsPerEmployee;

    @Override
    public List<EmployeeTag> getEmployeeTags(Long employeeId) {
        if (employeeId == null) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<EmployeeTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmployeeTag::getEmployeeId, employeeId)
               .eq(EmployeeTag::getIsActive, true)
               .and(w -> w.isNull(EmployeeTag::getExpireTime).or().gt(EmployeeTag::getExpireTime, LocalDateTime.now()))
               .orderByDesc(EmployeeTag::getId);
               
        return this.list(wrapper);
    }

    @Override
    public IPage<EmployeeTag> queryEmployeeTags(Page<EmployeeTag> page, Long employeeId, String dimension) {
        LambdaQueryWrapper<EmployeeTag> wrapper = new LambdaQueryWrapper<>();
        
        if (employeeId != null) {
            wrapper.eq(EmployeeTag::getEmployeeId, employeeId);
        }
        
        if (StringUtils.hasText(dimension)) {
            wrapper.eq(EmployeeTag::getDimension, dimension);
        }
        
        wrapper.eq(EmployeeTag::getIsActive, true)
               .and(w -> w.isNull(EmployeeTag::getExpireTime).or().gt(EmployeeTag::getExpireTime, LocalDateTime.now()))
               .orderByDesc(EmployeeTag::getId);
               
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addEmployeeTags(Long employeeId, List<EmployeeTag> tags) {
        if (employeeId == null || tags == null || tags.isEmpty()) {
            return false;
        }

        try {
            // 检查是否超过最大标签数
            long currentTagCount = this.count(new LambdaQueryWrapper<EmployeeTag>()
                    .eq(EmployeeTag::getEmployeeId, employeeId)
                    .eq(EmployeeTag::getIsActive, true)
                    .and(w -> w.isNull(EmployeeTag::getExpireTime).or().gt(EmployeeTag::getExpireTime, LocalDateTime.now())));

            if (currentTagCount + tags.size() > maxTagsPerEmployee) {
                log.error("员工标签数量超过限制：{}", maxTagsPerEmployee);
                return false;
            }

            // 批量保存标签
            for (EmployeeTag tag : tags) {
                tag.setEmployeeId(employeeId);
                tag.setIsActive(true);
            }
            
            return this.saveBatch(tags);
        } catch (Exception e) {
            log.error("添加员工标签失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeEmployeeTagsByDimension(Long employeeId, String dimension) {
        if (employeeId == null || !StringUtils.hasText(dimension)) {
            return false;
        }

        try {
            LambdaUpdateWrapper<EmployeeTag> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(EmployeeTag::getEmployeeId, employeeId)
                   .eq(EmployeeTag::getDimension, dimension)
                   .set(EmployeeTag::getIsActive, false);

            return this.update(wrapper);
        } catch (Exception e) {
            log.error("删除员工标签失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredTags() {
        try {
            LambdaUpdateWrapper<EmployeeTag> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(EmployeeTag::getIsActive, true)
                   .isNotNull(EmployeeTag::getExpireTime)
                   .lt(EmployeeTag::getExpireTime, LocalDateTime.now())
                   .set(EmployeeTag::getIsActive, false);
                   
            return this.baseMapper.update(null, wrapper);
        } catch (Exception e) {
            log.error("清理过期标签失败", e);
            return 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int persistRuleEngineTags(com.northland.drools.tagging.model.EnhancedEmployee employee) {
        if (employee == null || employee.getId() == null) {
            return 0;
        }

        List<EmployeeTag> ruleTags = new ArrayList<>();
        for (com.northland.drools.tagging.model.EmployeeTag modelTag : employee.getCurrentTags()) {
            if ("RULE_ENGINE".equals(modelTag.getSource()) && modelTag.isActive() && !modelTag.isExpired()) {
                ruleTags.add(toEntity(employee.getId(), modelTag));
            }
        }
        if (ruleTags.isEmpty()) {
            return 0;
        }

        // 失效数据库中该员工旧的规则标签，再保存本轮结果
        LambdaUpdateWrapper<EmployeeTag> deactivate = new LambdaUpdateWrapper<>();
        deactivate.eq(EmployeeTag::getEmployeeId, employee.getId())
                  .eq(EmployeeTag::getSource, "RULE_ENGINE")
                  .set(EmployeeTag::getIsActive, false);
        this.update(deactivate);

        this.saveBatch(ruleTags);
        log.info("员工 {} 规则标签落库 {} 条", employee.getId(), ruleTags.size());
        return ruleTags.size();
    }

    /**
     * Drools 工作内存对象（tagging.model）转持久化对象（entity）
     */
    private EmployeeTag toEntity(Long employeeId, com.northland.drools.tagging.model.EmployeeTag m) {
        EmployeeTag e = new EmployeeTag();
        e.setEmployeeId(employeeId);
        e.setTagCode(m.getTagCode());
        e.setTagName(m.getTagName());
        e.setCategory(m.getCategory() != null ? m.getCategory().name() : null);
        e.setDimension(m.getDimension());
        e.setConfidence(m.getConfidence());
        e.setSource(m.getSource());
        e.setReason(m.getReason());
        e.setExpireTime(m.getExpireTime());
        e.setIsActive(true);
        return e;
    }
}
