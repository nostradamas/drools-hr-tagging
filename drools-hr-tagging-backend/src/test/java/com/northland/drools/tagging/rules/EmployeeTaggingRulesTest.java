package com.northland.drools.tagging.rules;

import com.northland.drools.tagging.model.EnhancedEmployee;
import com.northland.drools.tagging.model.EmployeeTag;
import com.northland.drools.tagging.service.TagLibraryService;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 打标规则验证：DRL 可编译、档位互斥、标签来源与原因完整。
 * 不依赖 Spring 上下文，直接装配 KieContainer + 内存版标签库。
 */
class EmployeeTaggingRulesTest {

    private KieContainer buildContainer() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write(ResourceFactory.newClassPathResource("rules/employee-tagging-rules.drl"));
        kfs.write(ResourceFactory.newClassPathResource("rules/enhanced-employee-tagging-rules.drl"));
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        assertFalse(kb.getResults().hasMessages(Message.Level.ERROR),
                "DRL 编译失败: " + kb.getResults().getMessages(Message.Level.ERROR));
        return ks.newKieContainer(kb.getKieModule().getReleaseId());
    }

    private EnhancedEmployee employee(int totalWorkYears, int performance, int potential) {
        EnhancedEmployee e = new EnhancedEmployee();
        e.setId(1L);
        e.setName("测试员工");
        e.getExperienceInfo().setTotalWorkYears(totalWorkYears);
        e.getEvaluationInfo().setCurrentPerformanceScore(performance);
        e.getEvaluationInfo().setPotentialRating(potential);
        e.getEvaluationInfo().setGrowthTrend("上升");
        e.getEvaluationInfo().setTeamCollaborationScore(90);
        return e;
    }

    private List<EmployeeTag> fire(EnhancedEmployee e) {
        KieSession session = buildContainer().newKieSession();
        try {
            session.setGlobal("tagLibraryService", new TagLibraryService());
            session.insert(e);
            session.fireAllRules();
        } finally {
            session.dispose();
        }
        return e.getCurrentTags();
    }

    private static final List<String> EXPERIENCE_LADDER = Arrays.asList(
            "VETERAN_10PLUS", "SENIOR_5TO10", "MID_3TO5", "JUNIOR_1TO3", "FRESH_GRAD");
    private static final List<String> PERFORMANCE_LADDER = Arrays.asList(
            "TOP_PERFORMER", "HIGH_PERFORMER", "GOOD_PERFORMER", "AVERAGE_PERFORMER", "NEEDS_IMPROVEMENT");

    @Test
    void drlCompilesAndTagsAreProduced() {
        List<EmployeeTag> tags = fire(employee(6, 85, 4));
        assertFalse(tags.isEmpty(), "规则应至少产出一个标签");
        for (EmployeeTag tag : tags) {
            assertEquals("RULE_ENGINE", tag.getSource(), "标签来源应为 RULE_ENGINE");
            assertNotNull(tag.getReason(), "标签应带打标原因");
            assertNotNull(tag.getExpireTime(), "标签应带过期时间");
        }
    }

    @Test
    void experienceLadderIsExclusive() {
        for (int years : new int[]{0, 2, 4, 6, 12}) {
            List<String> codes = firedCodes(employee(years, 85, 3));
            long ladderHits = codes.stream().filter(EXPERIENCE_LADDER::contains).count();
            assertEquals(1, ladderHits, years + " 年经验应只命中一个档位，实际: " + codes);
        }
    }

    @Test
    void performanceLadderIsExclusive() {
        for (int score : new int[]{95, 85, 75, 65, 40}) {
            List<String> codes = firedCodes(employee(6, score, 3));
            long ladderHits = codes.stream().filter(PERFORMANCE_LADDER::contains).count();
            assertEquals(1, ladderHits, score + " 分绩效应只命中一个档位，实际: " + codes);
        }
    }

    @Test
    void veteranEmployeeGetsSeniorExpertTag() {
        List<String> codes = firedCodes(employee(10, 92, 5));
        assertTrue(codes.contains("VETERAN_10PLUS"), "10 年应命中资深专家: " + codes);
        assertFalse(codes.contains("SENIOR_5TO10"), "不应同时命中高级档: " + codes);
        assertTrue(codes.contains("TOP_PERFORMER"), "92 分应命中顶尖绩效: " + codes);
    }

    @Test
    void noDuplicateTagCodes() {
        for (int years : new int[]{0, 2, 4, 6, 12}) {
            List<String> codes = firedCodes(employee(years, 92, 5));
            assertEquals(codes.size(), new HashSet<>(codes).size(), "存在重复标签: " + codes);
        }
    }

    private List<String> firedCodes(EnhancedEmployee e) {
        return fire(e).stream().map(EmployeeTag::getTagCode).collect(Collectors.toList());
    }
}
