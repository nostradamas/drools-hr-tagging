package com.northland.drools.tagging.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.northland.drools.entity.Tag;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService extends IService<Tag> {
    
    /**
     * 分页查询标签
     *
     * @param page     分页参数
     * @param keyword  关键字
     * @param category 分类
     * @return 分页结果
     */
    IPage<Tag> queryTags(Page<Tag> page, String keyword, String category);
    
    /**
     * 根据分类获取标签列表
     *
     * @param category 分类名称
     * @return 标签列表
     */
    List<Tag> getTagsByCategory(String category);
    
    /**
     * 批量保存或更新标签
     *
     * @param tags 标签列表
     * @return 是否成功
     */
    boolean saveOrUpdateBatch(List<Tag> tags);
    
    /**
     * 检查标签名称是否已存在
     *
     * @param tagName 标签名称
     * @param excludeId 排除的标签ID
     * @return 是否存在
     */
    boolean checkTagNameExists(String tagName, Long excludeId);
}
