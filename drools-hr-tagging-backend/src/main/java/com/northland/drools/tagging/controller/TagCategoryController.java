package com.northland.drools.tagging.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.northland.drools.common.response.Result;
import com.northland.drools.entity.TagCategory;
import com.northland.drools.tagging.service.TagCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 标签分类管理Controller
 */
@RestController
@RequestMapping("/api/tag-categories")
@RequiredArgsConstructor
public class TagCategoryController {

    private final TagCategoryService tagCategoryService;

    /**
     * 分页查询标签分类
     */
    @GetMapping
    public Result<Page<TagCategory>> list(@RequestParam(defaultValue = "1") Integer current,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String keyword) {
        Page<TagCategory> page = tagCategoryService.page(new Page<>(current, size),
                new LambdaQueryWrapper<TagCategory>()
                        .like(keyword != null, TagCategory::getCategoryName, keyword)
                        .orderByDesc(TagCategory::getId));
        return Result.success(page);
    }

    /**
     * 添加标签分类
     */
    @PostMapping
    public Result<Void> add(@RequestBody TagCategory tagCategory) {
        tagCategoryService.save(tagCategory);
        return Result.success();
    }

    /**
     * 修改标签分类
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TagCategory tagCategory) {
        tagCategory.setId(id);
        tagCategoryService.updateById(tagCategory);
        return Result.success();
    }

    /**
     * 删除标签分类
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagCategoryService.removeById(id);
        return Result.success();
    }
}
