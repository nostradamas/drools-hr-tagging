package com.northland.drools.tagging.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.northland.drools.mapper.TagCategoryMapper;
import com.northland.drools.entity.TagCategory;
import com.northland.drools.tagging.service.TagCategoryService;
import org.springframework.stereotype.Service;

/**
 * 标签分类服务实现类
 */
@Service
public class TagCategoryServiceImpl extends ServiceImpl<TagCategoryMapper, TagCategory> implements TagCategoryService {
}
