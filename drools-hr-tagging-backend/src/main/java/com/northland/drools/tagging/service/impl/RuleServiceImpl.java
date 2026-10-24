package com.northland.drools.tagging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.northland.drools.mapper.TagRuleMapper;
import com.northland.drools.mapper.RuleTemplateMapper;
import com.northland.drools.entity.RuleTemplate;
import com.northland.drools.entity.TagRule;
import com.northland.drools.tagging.model.dto.RuleExecuteResult;
import com.northland.drools.tagging.model.dto.RuleValidateResult;
import com.northland.drools.tagging.service.RuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 规则服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl extends ServiceImpl<RuleTemplateMapper, RuleTemplate> implements RuleService {

    private final KieContainer kieContainer;

    private final com.northland.drools.tagging.service.DynamicRuleService dynamicRuleService;

    @Override
    public IPage<RuleTemplate> queryRuleTemplates(Page<RuleTemplate> page, String keyword) {
        LambdaQueryWrapper<RuleTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(RuleTemplate::getTemplateName, keyword)
                  .or()
                  .like(RuleTemplate::getDescription, keyword);
        }
        wrapper.orderByDesc(RuleTemplate::getId);
        return this.page(page, wrapper);
    }

    @Override
    public RuleValidateResult validateRuleContent(String ruleContent) {
        if (!StringUtils.hasText(ruleContent)) {
            return RuleValidateResult.error("规则内容不能为空");
        }

        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            
            String path = "src/main/resources/rules/temp_" + UUID.randomUUID() + ".drl";
            kieFileSystem.write(path, ruleContent);
            
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            Results results = kieBuilder.buildAll().getResults();
            
            if (results.hasMessages(Message.Level.ERROR)) {
                StringBuilder errorMsg = new StringBuilder();
                for (Message message : results.getMessages()) {
                    errorMsg.append(message.getText()).append("\\n");
                }
                return RuleValidateResult.error(errorMsg.toString());
            }
            
            return RuleValidateResult.success();
        } catch (Exception e) {
            log.error("验证规则内容失败", e);
            return RuleValidateResult.error("验证规则失败：" + e.getMessage());
        }
    }

    /**
     * 编译并加载规则（#16 修复）。
     *
     * <p>旧实现只做编译校验、编译成功后直接丢弃结果，规则从未进入任何容器。
     * 现在从 DRL 中解析规则名生成 ruleCode，委托 {@link com.northland.drools.tagging.service.DynamicRuleService}
     * 走"真编译校验 → 落库 → 重编译热加载"的完整通道，加载后即可通过动态规则会话执行。</p>
     *
     * @param ruleContent 完整的 DRL 文本，必须包含至少一条 {@code rule "名称"} 声明
     * @return 加载成功返回 true；内容为空、无规则名或校验/落库失败返回 false
     */
    @Override
    public boolean compileAndLoadRule(String ruleContent) {
        if (!StringUtils.hasText(ruleContent)) {
            return false;
        }

        String ruleName = extractFirstRuleName(ruleContent);
        if (ruleName == null) {
            log.error("编译并加载规则失败: DRL 中未找到 rule 声明");
            return false;
        }

        com.northland.drools.tagging.model.TagRule tagRule =
                new com.northland.drools.tagging.model.TagRule(
                        toRuleCode(ruleName), ruleName, "AD_HOC", "CUSTOM");
        tagRule.setRuleContent(ruleContent);
        tagRule.setDescription("经 compileAndLoadRule 加载的临时规则");

        boolean loaded = dynamicRuleService.addOrUpdateRule(tagRule);
        if (!loaded) {
            log.error("编译并加载规则 {} 失败", ruleName);
        }
        return loaded;
    }

    /** 从 DRL 文本中解析第一条规则的名称（rule "名称"）。 */
    private String extractFirstRuleName(String ruleContent) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("rule\\s+\"([^\"]+)\"")
                .matcher(ruleContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** 规则名 → ruleCode：非字母数字下划线替换为下划线并转大写。 */
    private String toRuleCode(String ruleName) {
        return "DYN_" + ruleName.replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
    }

    @Override
    public RuleExecuteResult executeRules(List<Object> facts) {
        if (facts == null || facts.isEmpty()) {
            return RuleExecuteResult.error("没有输入的事实对象");
        }

        KieSession kieSession = null;
        try {
            kieSession = kieContainer.newKieSession();
            List<Object> results = new ArrayList<>();
            
            // 插入事实对象
            for (Object fact : facts) {
                kieSession.insert(fact);
                results.add(fact);
            }
            
            // 触发规则
            int firedRules = kieSession.fireAllRules();
            log.info("执行了 {} 条规则", firedRules);
            
            return RuleExecuteResult.success(results);
        } catch (Exception e) {
            log.error("执行规则失败", e);
            return RuleExecuteResult.error("执行规则失败：" + e.getMessage());
        } finally {
            if (kieSession != null) {
                kieSession.dispose();
            }
        }
    }

    @Override
    public String getRuleContent(String templateName) {
        if (!StringUtils.hasText(templateName)) {
            return null;
        }
        
        LambdaQueryWrapper<RuleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleTemplate::getTemplateName, templateName);
        RuleTemplate template = this.getOne(wrapper);
        
        return template != null ? template.getRuleContent() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importRuleTemplates(List<RuleTemplate> templateList) {
        if (templateList == null || templateList.isEmpty()) {
            return false;
        }

        try {
            for (RuleTemplate template : templateList) {
                // 验证规则内容
                RuleValidateResult validateResult = validateRuleContent(template.getRuleContent());
                if (!validateResult.isValid()) {
                    log.error("规则模板 {} 验证失败: {}", template.getTemplateName(), validateResult.getErrorMessage());
                    return false;
                }
            }
            
            return this.saveBatch(templateList);
        } catch (Exception e) {
            log.error("导入规则模板失败", e);
            return false;
        }
    }
}
