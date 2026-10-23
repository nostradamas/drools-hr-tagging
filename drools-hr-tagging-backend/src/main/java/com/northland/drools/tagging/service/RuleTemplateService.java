package com.northland.drools.tagging.service;

import com.northland.drools.common.exception.BusinessException;
import com.northland.drools.tagging.model.RuleTemplate;
import com.northland.drools.tagging.model.TagRule;
import com.northland.drools.tagging.model.dto.RuleValidateResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 规则模板服务 - JDK 1.8兼容版本
 */
@Service
public class RuleTemplateService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 允许出现在生成 DRL 中的比较运算符白名单，其余一律回退为默认 {@code >=}（#14）。 */
    private static final Set<String> ALLOWED_OPERATORS = new HashSet<String>(
            Arrays.asList(">=", "<=", ">", "<", "==", "!=", "contains"));

    /**
     * 根据模板生成规则内容
     */
    public String generateRuleContent(TagRule tagRule) {
        RuleTemplate template = RuleTemplate.valueOf(tagRule.getRuleTemplate());
        Map<String, Object> conditions = parseConditions(tagRule.getConditions());

        switch (template) {
            case EXPERIENCE_BASED:
                return generateExperienceBasedRule(tagRule, conditions);
            case SKILL_BASED:
                return generateSkillBasedRule(tagRule, conditions);
            case PERFORMANCE_BASED:
                return generatePerformanceBasedRule(tagRule, conditions);
            case EDUCATION_BASED:
                return generateEducationBasedRule(tagRule, conditions);
            case LEADERSHIP_BASED:
                return generateLeadershipBasedRule(tagRule, conditions);
            case COMPOSITE:
                return generateCompositeRule(tagRule, conditions);
            case CUSTOM:
                return tagRule.getRuleContent(); // 直接返回自定义内容
            default:
                throw new IllegalArgumentException("不支持的规则模板: " + template);
        }
    }

    /** 公共 DRL 头部：包、导入、global、规则名（转义）与 salience。 */
    private StringBuilder ruleHeader(TagRule tagRule) {
        StringBuilder rule = new StringBuilder();
        rule.append("package com.northland.drools.tagging.rules\n\n");
        rule.append("import com.northland.drools.tagging.model.EnhancedEmployee\n");
        rule.append("import com.northland.drools.tagging.model.EmployeeTag\n");
        rule.append("import com.northland.drools.tagging.model.Tag\n");
        rule.append("import com.northland.drools.tagging.service.TagLibraryService\n\n");
        rule.append("global TagLibraryService tagLibraryService\n\n");
        rule.append("rule \"").append(escapeDrlString(tagRule.getRuleName())).append("\"\n");
        rule.append("    salience ").append(tagRule.getPriority()).append("\n");
        return rule;
    }

    /** 公共 then 段：按 tagCode（转义）查标签、去重打标。reasonExpression 为已拼好的 Java 表达式。 */
    private String thenBlock(TagRule tagRule, double confidence, String reasonExpression) {
        StringBuilder rule = new StringBuilder();
        rule.append("    then\n");
        rule.append("        Tag tag = tagLibraryService.getTagByCode(\"").append(escapeDrlString(tagRule.getTagCode())).append("\");\n");
        rule.append("        if (tag != null) {\n");
        rule.append("            EmployeeTag employeeTag = new EmployeeTag($employee.getId(), tag, ").append(confidence).append(", \"RULE_ENGINE\", \n");
        rule.append("                ").append(reasonExpression).append(");\n");
        rule.append("            $employee.addTag(employeeTag);\n");
        rule.append("            System.out.println(\"员工 \" + $employee.getName() + \" 获得标签：\" + tag.getName());\n");
        rule.append("        }\n");
        rule.append("end\n");
        return rule.toString();
    }

    /**
     * 生成经验年限规则
     */
    private String generateExperienceBasedRule(TagRule tagRule, Map<String, Object> conditions) {
        int minYears = getIntValue(conditions, "minYears", 0);
        int maxYears = getIntValue(conditions, "maxYears", 50);
        String operator = sanitizeOperator(getStringValue(conditions, "operator", ">="));

        StringBuilder rule = ruleHeader(tagRule);
        rule.append("    when\n");
        rule.append("        $employee : EnhancedEmployee()\n");

        if (maxYears < 50) {
            rule.append("        eval($employee.getExperienceInfo().getTotalWorkYears() ")
                .append(operator).append(" ").append(minYears)
                .append(" && $employee.getExperienceInfo().getTotalWorkYears() <= ")
                .append(maxYears).append(")\n");
        } else {
            rule.append("        eval($employee.getExperienceInfo().getTotalWorkYears() ")
                .append(operator).append(" ").append(minYears).append(")\n");
        }

        rule.append(thenBlock(tagRule, 0.8, "\"工作经验\" + $employee.getExperienceInfo().getTotalWorkYears() + \"年\""));
        return rule.toString();
    }

    /**
     * 生成技能规则
     */
    private String generateSkillBasedRule(TagRule tagRule, Map<String, Object> conditions) {
        List<String> requiredSkills = getStringListValue(conditions, "requiredSkills");
        String matchType = getStringValue(conditions, "matchType", "ANY");
        if (requiredSkills.isEmpty()) {
            throw new BusinessException("技能规则至少需要配置一个 requiredSkills 条件");
        }

        StringBuilder rule = ruleHeader(tagRule);
        rule.append("    when\n");
        rule.append("        $employee : EnhancedEmployee()\n");
        rule.append("        $ability : EnhancedEmployee.AbilityInfo() from $employee.getAbilityInfo()\n");

        if ("ALL".equals(matchType)) {
            // 必须包含所有技能
            for (String skill : requiredSkills) {
                rule.append("        eval($ability.getTechnicalSkills().contains(\"").append(escapeDrlString(skill)).append("\"))\n");
            }
        } else {
            // 包含任意技能
            rule.append("        eval(");
            for (int i = 0; i < requiredSkills.size(); i++) {
                if (i > 0) rule.append(" || ");
                rule.append("$ability.getTechnicalSkills().contains(\"").append(escapeDrlString(requiredSkills.get(i))).append("\")");
            }
            rule.append(")\n");
        }

        rule.append(thenBlock(tagRule, 0.8, "\"具备相关技能\""));
        return rule.toString();
    }

    /**
     * 生成绩效规则
     */
    private String generatePerformanceBasedRule(TagRule tagRule, Map<String, Object> conditions) {
        int minScore = getIntValue(conditions, "minScore", 0);
        int maxScore = getIntValue(conditions, "maxScore", 100);
        String operator = sanitizeOperator(getStringValue(conditions, "operator", ">="));

        StringBuilder rule = ruleHeader(tagRule);
        rule.append("    when\n");
        rule.append("        $employee : EnhancedEmployee()\n");
        rule.append("        $evaluation : EnhancedEmployee.EvaluationInfo() from $employee.getEvaluationInfo()\n");

        if (maxScore < 100) {
            rule.append("        eval($evaluation.getCurrentPerformanceScore() ")
                .append(operator).append(" ").append(minScore)
                .append(" && $evaluation.getCurrentPerformanceScore() <= ")
                .append(maxScore).append(")\n");
        } else {
            rule.append("        eval($evaluation.getCurrentPerformanceScore() ")
                .append(operator).append(" ").append(minScore).append(")\n");
        }

        rule.append(thenBlock(tagRule, 0.9, "\"绩效评分\" + $evaluation.getCurrentPerformanceScore()"));
        return rule.toString();
    }

    /**
     * 生成学历规则
     */
    private String generateEducationBasedRule(TagRule tagRule, Map<String, Object> conditions) {
        List<String> educationLevels = getStringListValue(conditions, "educationLevels");
        if (educationLevels.isEmpty()) {
            educationLevels = new ArrayList<String>(Arrays.asList("本科"));
        }

        StringBuilder rule = ruleHeader(tagRule);
        rule.append("    when\n");
        rule.append("        $employee : EnhancedEmployee()\n");
        rule.append("        eval(");

        for (int i = 0; i < educationLevels.size(); i++) {
            if (i > 0) rule.append(" || ");
            rule.append("\"").append(escapeDrlString(educationLevels.get(i))).append("\".equals($employee.getExperienceInfo().getEducation())");
        }
        rule.append(")\n");

        rule.append(thenBlock(tagRule, 0.8, "\"学历：\" + $employee.getExperienceInfo().getEducation()"));
        return rule.toString();
    }

    /**
     * 生成领导力规则
     */
    private String generateLeadershipBasedRule(TagRule tagRule, Map<String, Object> conditions) {
        int minLeadershipScore = getIntValue(conditions, "minLeadershipScore", 70);
        int minManagementYears = getIntValue(conditions, "minManagementYears", 1);
        int minTeamSize = getIntValue(conditions, "minTeamSize", 3);

        StringBuilder rule = ruleHeader(tagRule);
        rule.append("    when\n");
        rule.append("        $employee : EnhancedEmployee()\n");
        rule.append("        $ability : EnhancedEmployee.AbilityInfo() from $employee.getAbilityInfo()\n");
        rule.append("        eval($ability.getLeadershipScore() >= ").append(minLeadershipScore).append(" && \n");
        rule.append("             $employee.getExperienceInfo().getManagementYears() >= ").append(minManagementYears).append(" &&\n");
        rule.append("             $employee.getExperienceInfo().getTeamSize() >= ").append(minTeamSize).append(")\n");

        rule.append(thenBlock(tagRule, 0.9,
                "\"领导力评分\" + $ability.getLeadershipScore() + \"，管理经验\" + $employee.getExperienceInfo().getManagementYears() + \"年\""));
        return rule.toString();
    }

    /**
     * 生成复合条件规则。
     *
     * <p>支持的条件键：minYears / maxYears（履历年限）、minScore（绩效，operator 可选）、
     * requiredSkills（技能，任意命中）、minSkills（技能数量）、educationLevels（学历）、
     * minLeadershipScore（领导力）。所有片段以 AND 组合。
     *
     * <p>若配置产生不了任何条件片段，直接抛 {@link BusinessException}（#14）：
     * 绝不生成 when 为空的规则——空 when 会无条件给所有员工打标。
     */
    private String generateCompositeRule(TagRule tagRule, Map<String, Object> conditions) {
        List<String> fragments = new ArrayList<String>();
        boolean needAbility = false;
        boolean needEvaluation = false;

        Integer minYears = getIntValueOrNull(conditions, "minYears");
        if (minYears != null) {
            String expr = "$employee.getExperienceInfo().getTotalWorkYears() >= " + minYears;
            Integer maxYears = getIntValueOrNull(conditions, "maxYears");
            if (maxYears != null) {
                expr += " && $employee.getExperienceInfo().getTotalWorkYears() <= " + maxYears;
            }
            fragments.add(expr);
        }

        List<String> requiredSkills = getStringListValue(conditions, "requiredSkills");
        if (!requiredSkills.isEmpty()) {
            needAbility = true;
            StringBuilder expr = new StringBuilder();
            for (int i = 0; i < requiredSkills.size(); i++) {
                if (i > 0) expr.append(" || ");
                expr.append("$ability.getTechnicalSkills().contains(\"")
                    .append(escapeDrlString(requiredSkills.get(i))).append("\")");
            }
            fragments.add(expr.toString());
        }

        Integer minSkills = getIntValueOrNull(conditions, "minSkills");
        if (minSkills != null) {
            needAbility = true;
            fragments.add("$ability.getTechnicalSkills().size() >= " + minSkills);
        }

        Integer minScore = getIntValueOrNull(conditions, "minScore");
        if (minScore != null) {
            needEvaluation = true;
            String operator = sanitizeOperator(getStringValue(conditions, "operator", ">="));
            fragments.add("$evaluation.getCurrentPerformanceScore() " + operator + " " + minScore);
        }

        List<String> educationLevels = getStringListValue(conditions, "educationLevels");
        if (!educationLevels.isEmpty()) {
            StringBuilder expr = new StringBuilder();
            for (int i = 0; i < educationLevels.size(); i++) {
                if (i > 0) expr.append(" || ");
                expr.append("\"").append(escapeDrlString(educationLevels.get(i)))
                    .append("\".equals($employee.getExperienceInfo().getEducation())");
            }
            fragments.add(expr.toString());
        }

        Integer minLeadershipScore = getIntValueOrNull(conditions, "minLeadershipScore");
        if (minLeadershipScore != null) {
            needAbility = true;
            fragments.add("$ability.getLeadershipScore() >= " + minLeadershipScore);
        }

        if (fragments.isEmpty()) {
            throw new BusinessException("复合规则至少需要一个有效条件（支持 minYears、minScore、requiredSkills、minSkills、educationLevels、minLeadershipScore），"
                    + "当前条件无法生成任何条件片段，已拒绝生成空 when 规则: " + tagRule.getRuleCode());
        }

        StringBuilder rule = ruleHeader(tagRule);
        rule.append("    when\n");
        rule.append("        $employee : EnhancedEmployee()\n");
        if (needAbility) {
            rule.append("        $ability : EnhancedEmployee.AbilityInfo() from $employee.getAbilityInfo()\n");
        }
        if (needEvaluation) {
            rule.append("        $evaluation : EnhancedEmployee.EvaluationInfo() from $employee.getEvaluationInfo()\n");
        }
        for (String fragment : fragments) {
            rule.append("        eval(").append(fragment).append(")\n");
        }

        rule.append(thenBlock(tagRule, 0.8, "\"复合条件匹配\""));
        return rule.toString();
    }

    /**
     * 解析条件JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConditions(String conditionsJson) {
        try {
            if (conditionsJson == null || conditionsJson.trim().isEmpty()) {
                return new HashMap<String, Object>();
            }
            return objectMapper.readValue(conditionsJson, Map.class);
        } catch (Exception e) {
            System.err.println("解析条件JSON失败: " + e.getMessage());
            return new HashMap<String, Object>();
        }
    }

    // ---------- 类型安全的条件取值（#14） ----------
    // 条件来自 Jackson 反序列化：数组是 List、数字是 Integer/Long/Double、字符串是 String。
    // 此前代码把 List 直接强转为 String[]，条件里只要带技能/学历数组必 ClassCastException。

    /** 取整数条件，兼容 Number 与数字字符串，缺失/不可解析时返回默认值。 */
    private int getIntValue(Map<String, Object> conditions, String key, int defaultValue) {
        Integer value = getIntValueOrNull(conditions, key);
        return value != null ? value : defaultValue;
    }

    private Integer getIntValueOrNull(Map<String, Object> conditions, String key) {
        Object raw = conditions.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        if (raw instanceof String) {
            try {
                return Integer.valueOf(((String) raw).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** 取字符串条件，缺失时返回默认值。 */
    private String getStringValue(Map<String, Object> conditions, String key, String defaultValue) {
        Object raw = conditions.get(key);
        return raw != null ? String.valueOf(raw) : defaultValue;
    }

    /**
     * 取字符串列表条件：兼容单个字符串、JSON 数组（List）、Java 数组三种输入形态，
     * 元素统一 String.valueOf；缺失时返回空列表。
     */
    private List<String> getStringListValue(Map<String, Object> conditions, String key) {
        List<String> result = new ArrayList<String>();
        Object raw = conditions.get(key);
        if (raw == null) {
            return result;
        }
        if (raw instanceof Collection) {
            for (Object item : (Collection<?>) raw) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        } else if (raw.getClass().isArray()) {
            int len = Array.getLength(raw);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(raw, i);
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        } else {
            result.add(String.valueOf(raw));
        }
        return result;
    }

    /** 运算符白名单：不在白名单内的运算符一律回退为 {@code >=}，防止把任意字符串拼进 DRL。 */
    private String sanitizeOperator(String operator) {
        if (operator != null && ALLOWED_OPERATORS.contains(operator.trim())) {
            return operator.trim();
        }
        return ">=";
    }

    /** 转义将嵌入 DRL 字符串字面量的文本（规则名、标签编码、技能/学历值）：反斜杠与双引号及控制字符。 */
    private String escapeDrlString(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 验证规则语法：真实编译校验（#14）。
     *
     * <p>此前实现只做 contains("rule"/"when"/"then"/"end") 字符串检查，
     * 语法错误、类型错误都能通过。现在与 {@code RuleServiceImpl.validateRuleContent}
     * 同一路径：用 KieBuilder 实际编译，收集 ERROR 级消息作为失败依据。
     */
    public RuleValidateResult validateRuleContent(String ruleContent) {
        if (ruleContent == null || ruleContent.trim().isEmpty()) {
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
                for (Message message : results.getMessages(Message.Level.ERROR)) {
                    errorMsg.append(message.getText()).append("\n");
                }
                return RuleValidateResult.error(errorMsg.toString());
            }
            return RuleValidateResult.success();
        } catch (Exception e) {
            return RuleValidateResult.error("验证规则失败：" + e.getMessage());
        }
    }

    /** 兼容既有调用方的 boolean 签名，语义与 {@link #validateRuleContent} 一致。 */
    public boolean validateRule(String ruleContent) {
        return validateRuleContent(ruleContent).isValid();
    }

    /**
     * 获取所有可用的模板
     */
    public RuleTemplate[] getAllTemplates() {
        return RuleTemplate.values();
    }
}
