package com.northland.drools.tagging.service;

import com.northland.drools.entity.TagRule;
import com.northland.drools.mapper.TagRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * 动态规则服务测试（#16）：用内存版 TagRuleMapper 替身模拟 tag_rule 表，验证
 * 1) 新增规则先落库、随后建会话能真正命中打标；
 * 2) 重建服务实例并从 DB 重新加载后（模拟重启），规则仍能命中 —— 落库生效；
 * 3) 编译不过的规则被拒绝且不落库、不污染现有容器；
 * 4) 删除规则落库后，会话不再命中该规则。
 *
 * 不依赖 Spring 上下文与真实 MySQL。
 */
class DynamicRuleServiceTest {

    private static final String RULE_CODE = "RULE_DYN_HIGH_SCORE";

    private static final String RULE_DRL =
            "package rules.dynamic;\n" +
            "import com.northland.drools.tagging.service.TestScoreFact;\n" +
            "global com.northland.drools.tagging.service.TagLibraryService tagLibraryService;\n" +
            "rule \"DynHighScore\"\n" +
            "when\n" +
            "    $f : TestScoreFact( score >= 90, level == null )\n" +
            "then\n" +
            "    $f.setLevel(\"HIGH\");\n" +
            "end\n";

    /** 内存版 tag_rule 表：ruleCode -> entity。 */
    private Map<String, TagRule> table;
    private TagRuleMapper mapper;

    @BeforeEach
    void setUp() {
        table = new LinkedHashMap<String, TagRule>();
        final AtomicLong idSeq = new AtomicLong(0);
        mapper = mock(TagRuleMapper.class);

        doAnswer(inv -> new ArrayList<TagRule>(table.values()))
                .when(mapper).findAllRules();
        doAnswer(inv -> table.get(inv.getArgument(0)))
                .when(mapper).findByRuleCode(any());
        doAnswer(inv -> {
            TagRule e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(idSeq.incrementAndGet());
            }
            table.put(e.getRuleCode(), e);
            return 1;
        }).when(mapper).insert(any(TagRule.class));
        doAnswer(inv -> {
            TagRule e = inv.getArgument(0);
            table.put(e.getRuleCode(), e);
            return 1;
        }).when(mapper).updateById(any(TagRule.class));
        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            table.values().removeIf(e -> id.equals(e.getId()));
            return 1;
        }).when(mapper).deleteById(anyLong());
        doAnswer(inv -> 1).when(mapper).updateExecutionCount(any());
    }

    private DynamicRuleService newService() {
        return new DynamicRuleService(mapper, new RuleTemplateService(), new TagLibraryService());
    }

    private com.northland.drools.tagging.model.TagRule highScoreRule() {
        com.northland.drools.tagging.model.TagRule rule = new com.northland.drools.tagging.model.TagRule(RULE_CODE, "高分规则", "HIGH_SCORE", "CUSTOM");
        rule.setRuleContent(RULE_DRL);
        rule.setPriority(9);
        return rule;
    }

    private String fireAndGetLevel(DynamicRuleService service, int score) {
        KieSession session = service.createKieSession();
        assertNotNull(session, "KieSession 不应为空");
        try {
            TestScoreFact fact = new TestScoreFact(score);
            session.insert(fact);
            session.fireAllRules();
            return fact.getLevel();
        } finally {
            session.dispose();
        }
    }

    @Test
    void addedRuleIsPersistedAndFires() {
        DynamicRuleService service = newService();
        service.loadRulesFromDatabase();

        assertTrue(service.addOrUpdateRule(highScoreRule()), "新增规则应成功");
        assertTrue(table.containsKey(RULE_CODE), "规则必须先落库（tag_rule 中应有记录）");

        assertEquals("HIGH", fireAndGetLevel(service, 95), "95 分应命中动态规则");
        assertNull(fireAndGetLevel(service, 50), "50 分不应命中");
    }

    @Test
    void ruleSurvivesServiceRecreation() {
        DynamicRuleService first = newService();
        first.loadRulesFromDatabase();
        assertTrue(first.addOrUpdateRule(highScoreRule()));

        // 模拟重启：全新服务实例，只从 DB 加载
        DynamicRuleService restarted = newService();
        restarted.loadRulesFromDatabase();

        assertNotNull(restarted.getRule(RULE_CODE), "重启后规则应从 DB 恢复");
        assertEquals("HIGH", fireAndGetLevel(restarted, 99), "重启后规则仍应能命中打标");
    }

    @Test
    void invalidRuleIsRejectedAndNotPersisted() {
        DynamicRuleService service = newService();
        service.loadRulesFromDatabase();
        assertTrue(service.addOrUpdateRule(highScoreRule()));

        com.northland.drools.tagging.model.TagRule bad = new com.northland.drools.tagging.model.TagRule("RULE_BAD", "坏规则", "BAD", "CUSTOM");
        bad.setRuleContent("this is not valid drl {{{");
        assertFalse(service.addOrUpdateRule(bad), "编译不过的规则必须被拒绝");
        assertFalse(table.containsKey("RULE_BAD"), "被拒绝的规则不得落库");
        assertEquals("HIGH", fireAndGetLevel(service, 95), "坏规则不得污染现有容器");
    }

    @Test
    void deletedRuleNoLongerFires() {
        DynamicRuleService service = newService();
        service.loadRulesFromDatabase();
        assertTrue(service.addOrUpdateRule(highScoreRule()));
        assertEquals("HIGH", fireAndGetLevel(service, 95));

        assertTrue(service.deleteRule(RULE_CODE), "删除应成功");
        assertFalse(table.containsKey(RULE_CODE), "删除必须同步落库");
        assertNull(fireAndGetLevel(service, 95), "删除后规则不应再命中");
    }

    @Test
    void disabledRuleDoesNotFireButStaysPersisted() {
        DynamicRuleService service = newService();
        service.loadRulesFromDatabase();
        assertTrue(service.addOrUpdateRule(highScoreRule()));

        assertTrue(service.toggleRule(RULE_CODE, false), "停用应成功");
        assertTrue(table.containsKey(RULE_CODE), "停用不等于删除，记录应保留");
        assertNull(fireAndGetLevel(service, 95), "停用的规则不应命中");
    }
}
