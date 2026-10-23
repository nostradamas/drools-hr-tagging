package com.northland.drools.tagging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.northland.drools.mapper.EmployeeTagMapper;
import com.northland.drools.entity.EmployeeTag;
import com.northland.drools.tagging.service.EmployeeTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
               .eq(EmployeeTag::getActive, true)
               .eq(EmployeeTag::getExpired, false)
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
        
        wrapper.eq(EmployeeTag::getActive, true)
               .eq(EmployeeTag::getExpired, false)
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
                    .eq(EmployeeTag::getEmployeeId, employeeId != null ? employeeId : null)
                    .eq(EmployeeTag::getActive, true)
                    .eq(EmployeeTag::getExpired, false));

            if (currentTagCount + tags.size() > maxTagsPerEmployee) {
                log.error("员工标签数量超过限制：{}", maxTagsPerEmployee);
                return false;
            }

            // 批量保存标签
            for (EmployeeTag tag : tags) {
                tag.setEmployeeId(employeeId);
                tag.setActive(true);
                tag.setExpired(false);
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
                   .set(EmployeeTag::getActive, false);
                   
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
            wrapper.eq(EmployeeTag::getActive, true)
                   .eq(EmployeeTag::getExpired, true)
                   .set(EmployeeTag::getActive, false);
                   
            return this.baseMapper.update(null, wrapper);
        } catch (Exception e) {
            log.error("清理过期标签失败", e);
            return 0;
        }
    }
}
