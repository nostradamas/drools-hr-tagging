package com.northland.drools.tagging.service;

import com.northland.drools.common.exception.BusinessException;
import com.northland.drools.tagging.model.TagRule;
import com.northland.drools.tagging.model.dto.RuleValidateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RuleTemplateService 验证（#14）：
 * validateRule 走真实 KieBuilder 编译而非字符串包含检查；
 * 复合模板无有效条件时拒绝生成空 when 规则；
 * 条件中的技能/学历数组不再因 (String[]) 强转抛 ClassCastException。
 * 不起 Spring 上下文，直接 new 服务类。
 */
class RuleTemplateServiceTest {

    private RuleTemplateService service;

    @BeforeEach
    void setUp() {
        service = new RuleTemplateService();
    }

    private TagRule rule(String template, String conditions) {
        TagRule rule = new TagRule("RULE_TEST", "测试规则", "TEST_TAG", template);
        rule.setConditions(conditions);
        rule.setPriority(5);
        return rule;
    }

    @Test
    void validateRuleRejectsSyntacticallyInvalidDrl() {
        // 旧实现只查 contains("rule"/"when"/"then"/"end")，下面这段能全部命中却无法编译
        String invalid = "package com.northland.drools.tagging.rules\n"
                + "rule \"broken\"\n"
                + "    when\n"
                + "        $employee : NoSuchClass()\n"
                + "    then\n"
                + "        System.out.println(\"x\");\n"
                + "end\n";
        assertTrue(invalid.contains("rule") && invalid.contains("when")
                && invalid.contains("then") && invalid.contains("end"));

        assertFalse(service.validateRule(invalid), "引用不存在的类应被真实编译拒绝");
        RuleValidateResult result = service.validateRuleContent(invalid);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertFalse(result.getErrorMessage().isEmpty(), "失败时应给出编译错误信息");
    }

    @Test
    void validateRuleAcceptsValidRule() {
        String generated = service.generateRuleContent(
                rule("EXPERIENCE_BASED", "{\"minYears\": 5, \"operator\": \">=\"}"));
        assertTrue(service.validateRule(generated), "模板生成的合法规则应通过真实编译: " + generated);
        assertTrue(service.validateRuleContent(generated).isValid());
    }

    @Test
    void validateRuleRejectsEmptyContent() {
        assertFalse(service.validateRule(null));
        assertFalse(service.validateRule(""));
        assertFalse(service.validateRuleContent("  ").isValid());
    }

    @Test
    void compositeWithEmptyConditionsThrows() {
        assertThrows(BusinessException.class,
                () -> service.generateRuleContent(rule("COMPOSITE", "{}")),
                "空条件不得生成 when 为空的复合规则");
        assertThrows(BusinessException.class,
                () -> service.generateRuleContent(rule("COMPOSITE", null)),
                "无条件不得生成 when 为空的复合规则");
        assertThrows(BusinessException.class,
                () -> service.generateRuleContent(rule("COMPOSITE", "{\"unknownKey\": 1}")),
                "只有未知键、产生不了任何条件片段时同样应拒绝");
    }

    @Test
    void compositeWithValidConditionsGeneratesCompilableRule() {
        String generated = assertDoesNotThrow(() -> service.generateRuleContent(
                rule("COMPOSITE", "{\"minYears\": 8, \"requiredSkills\": [\"Java\"], \"minScore\": 85}")));
        assertTrue(generated.contains("eval("), "复合规则应包含真实条件片段: " + generated);
        assertTrue(service.validateRule(generated), "复合规则应能通过真实编译: " + generated);
    }

    @Test
    void skillArrayConditionsDoNotThrowClassCastException() {
        // Jackson 把 JSON 数组解析为 List，此前 (String[]) 强转必抛 ClassCastException
        String generated = assertDoesNotThrow(() -> service.generateRuleContent(
                rule("SKILL_BASED", "{\"requiredSkills\": [\"Java\", \"JavaScript\"], \"matchType\": \"ALL\"}")));
        assertTrue(generated.contains("\"Java\""));
        assertTrue(generated.contains("\"JavaScript\""));
        assertTrue(service.validateRule(generated), "技能模板生成结果应能通过真实编译: " + generated);
    }

    @Test
    void educationArrayConditionsDoNotThrowClassCastException() {
        String generated = assertDoesNotThrow(() -> service.generateRuleContent(
                rule("EDUCATION_BASED", "{\"educationLevels\": [\"博士\", \"硕士\"], \"matchType\": \"ANY\"}")));
        assertTrue(generated.contains("博士"));
        assertTrue(service.validateRule(generated), "学历模板生成结果应能通过真实编译: " + generated);
    }
}
