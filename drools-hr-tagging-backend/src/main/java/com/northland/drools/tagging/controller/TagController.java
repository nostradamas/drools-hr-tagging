package com.northland.drools.tagging.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.northland.drools.common.response.ApiResponse;
import com.northland.drools.entity.Tag;
import com.northland.drools.tagging.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 标签管理Controller
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 分页查询标签
     */
    @GetMapping
    public ApiResponse<Page<Tag>> list(@RequestParam(defaultValue = "1") Integer current,
                                 @RequestParam(defaultValue = "10") Integer size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String category) {
        Page<Tag> page = tagService.page(new Page<>(current, size),
                new LambdaQueryWrapper<Tag>()
                        .like(keyword != null, Tag::getName, keyword)
                        .eq(category != null, Tag::getCategory, category)
                        .orderByDesc(Tag::getId));
        return ApiResponse.success(page);
    }

    /**
     * 添加标签
     */
    @PostMapping
    public ApiResponse<Void> add(@RequestBody Tag tag) {
        tagService.save(tag);
        return ApiResponse.ok();
    }

    /**
     * 修改标签
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Tag tag) {
        tag.setId(id);
        tagService.updateById(tag);
        return ApiResponse.ok();
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.removeById(id);
        return ApiResponse.ok();
    }

    /**
     * 根据分类获取标签列表
     */
    @GetMapping("/by-category/{category}")
    public ApiResponse<Page<Tag>> listByCategory(@PathVariable String category,
                                          @RequestParam(defaultValue = "1") Integer current,
                                          @RequestParam(defaultValue = "10") Integer size) {
        Page<Tag> page = tagService.page(new Page<>(current, size),
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getCategory, category)
                        .orderByDesc(Tag::getId));
        return ApiResponse.success(page);
    }
}
