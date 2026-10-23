package com.northland.drools.tagging.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.northland.drools.entity.RuleTemplate;
import com.northland.drools.entity.TagRule;
import com.northland.drools.tagging.model.dto.RuleExecuteResult;
import com.northland.drools.tagging.model.dto.RuleValidateResult;

import java.util.List;

/**
 * 规则服务接口
 */
public interface RuleService extends IService<RuleTemplate> {
    
    /**
     * 分页查询规则模板
     *
     * @param page    分页参数
     * @param keyword 关键字
     * @return 分页结果
     */
    IPage<RuleTemplate> queryRuleTemplates(Page<RuleTemplate> page, String keyword);
    
    /**
     * 验证规则内容语法
     *
     * @param ruleContent 规则内容
     * @return 验证结果，包含是否有效和错误信息
     */
    RuleValidateResult validateRuleContent(String ruleContent);
    
    /**
     * 编译并加载规则
     *
     * @param ruleContent 规则内容
     * @return 是否成功
     */
    boolean compileAndLoadRule(String ruleContent);
    
    /**
     * 执行规则
     *
     * @param facts 事实对象列表
     * @return 执行结果
     */
    RuleExecuteResult executeRules(List<Object> facts);
    
    /**
     * 获取规则模板的内容
     *
     * @param templateName 模板名称
     * @return 规则内容
     */
    String getRuleContent(String templateName);
    
    /**
     * 导入规则模板
     *
     * @param templateList 规则模板列表
     * @return 是否成功
     */
    boolean importRuleTemplates(List<RuleTemplate> templateList);
}
