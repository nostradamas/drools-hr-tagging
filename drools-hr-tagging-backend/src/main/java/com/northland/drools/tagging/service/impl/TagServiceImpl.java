package com.northland.drools.tagging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.northland.drools.mapper.TagMapper;
import com.northland.drools.entity.Tag;
import com.northland.drools.tagging.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签服务实现类
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public IPage<Tag> queryTags(Page<Tag> page, String keyword, String category) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Tag::getName, keyword);
        }
        
        if (StringUtils.hasText(category)) {
            wrapper.eq(Tag::getCategory, category);
        }
        
        wrapper.orderByDesc(Tag::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<Tag> getTagsByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getCategory, category)
               .orderByDesc(Tag::getId);
               
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        return this.saveOrUpdateBatch(tags, 100);
    }

    @Override
    public boolean checkTagNameExists(String tagName, Long excludeId) {
        if (!StringUtils.hasText(tagName)) {
            return false;
        }
        
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, tagName);
        
        if (excludeId != null) {
            wrapper.ne(Tag::getId, excludeId);
        }
        
        return this.count(wrapper) > 0;
    }
}
