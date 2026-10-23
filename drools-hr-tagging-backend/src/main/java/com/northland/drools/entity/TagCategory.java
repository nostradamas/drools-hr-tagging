package com.northland.drools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 标签分类实体类
 */
@Data
@Accessors(chain = true)
@TableName("tag_category")
public class TagCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private String description;
}
