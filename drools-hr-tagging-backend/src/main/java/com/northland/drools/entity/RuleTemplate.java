package com.northland.drools.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 规则模板实体类
 */
@Data
@Accessors(chain = true)
@TableName("rule_template")
public class RuleTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String description;
    private String ruleContent;
}
