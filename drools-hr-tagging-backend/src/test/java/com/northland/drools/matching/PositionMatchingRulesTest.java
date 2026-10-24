package com.northland.drools.matching;

import com.northland.drools.matching.model.Employee;
import com.northland.drools.matching.model.MatchingResult;
import com.northland.drools.matching.model.Position;
import com.northland.drools.matching.service.EmployeeMatchingService;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 岗位匹配规则验证（#18）：DRL 可编译、五维评分累加、总分封顶 100、不匹配原因留痕。
 * 评分构成：经验 20 + 必需技能 30（部分按比例 20）+ 优选技能 15（按比例）
 * + 学历 15 + 薪资 10 + 管理 10 + 高绩效 5 + 级别 5，原始满分 110，靠封顶回 100。
 * 不依赖 Spring 上下文，直接装配 KieContainer 后经 EmployeeMatchingService 执行。
 */
class PositionMatchingRulesTest {

    private KieContainer buildContainer() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write(ResourceFactory.newClassPathResource("rules/position-matching-rules.drl"));
        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();
        assertFalse(kb.getResults().hasMessages(Message.Level.ERROR),
                "岗位匹配 DRL 编译失败: " + kb.getResults().getMessages(Message.Level.ERROR));
        return ks.newKieContainer(kb.getKieModule().getReleaseId());
    }

    /** 走真实服务路径（含两阶段 agenda-group 执行），与生产调用方式一致。 */
    private MatchingResult fire(Employee employee, Position position) {
        EmployeeMatchingService service = new EmployeeMatchingService();
        ReflectionTestUtils.setField(service, "kieContainer", buildContainer());
        return service.matchEmployeeToPosition(employee, position);
    }

    private Position seniorJavaPosition() {
        Position p = new Position();
        p.setTitle("高级 Java 工程师");
        p.setRequiredSkills(Arrays.asList("Java", "Spring"));
        p.setPreferredSkills(Arrays.asList("Docker"));
        p.setMinExperience(3);
        p.setMaxExperience(10);
        p.setMinEducation("本科");
        p.setMinSalary(20000);
        p.setMaxSalary(40000);
        p.setLevel("高级");
        p.setRequiresManagement(true);
        return p;
    }

    private Employee employee(int experience, String education, double expectedSalary,
                              int performance, boolean management, String... skills) {
        Employee e = new Employee();
        e.setName("测试员工");
        e.setWorkExperience(experience);
        e.setEducation(education);
        e.setExpectedSalary(expectedSalary);
        e.setPerformanceScore(performance);
        e.setHasManagementExperience(management);
        e.setSkills(Arrays.asList(skills));
        return e;
    }

    @Test
    void fullMatchIsCappedAt100AndHighlyRecommended() {
        // 五维全满：20 + 30 + 15 + 15 + 10 + 10 + 高绩效 5 + 级别 5 = 110 → 封顶 100
        Employee e = employee(6, "硕士", 30000, 90, true, "Java", "Spring", "Docker");
        MatchingResult r = fire(e, seniorJavaPosition());

        assertEquals(100.0, r.getMatchScore(), 0.001, "原始分超过 100 时必须封顶");
        assertTrue(r.isMatch());
        assertTrue(r.isRecommended());
        assertEquals("强烈推荐：匹配度很高", r.getRecommendation());
        assertTrue(r.getMismatchReasons().isEmpty(), "全匹配不应有不匹配原因: " + r.getMismatchReasons());
    }

    @Test
    void partialSkillMatchScoresProportionally() {
        Position p = new Position();
        p.setTitle("中级工程师");
        p.setRequiredSkills(Arrays.asList("Java", "Spring", "MySQL", "Redis"));
        p.setPreferredSkills(Arrays.asList("Docker"));
        p.setMinExperience(2);
        p.setMaxExperience(8);
        p.setMinEducation("本科");
        p.setMinSalary(15000);
        p.setMaxSalary(30000);
        p.setLevel("中级");
        p.setRequiresManagement(false);

        // 经验 20 + 必需技能 2/4 按比例 10 + 学历 15 + 薪资 10 + 级别 5 = 60
        Employee e = employee(3, "本科", 20000, 70, false, "Java", "Spring");
        MatchingResult r = fire(e, p);

        assertEquals(60.0, r.getMatchScore(), 0.001);
        assertTrue(r.isMatch(), "60 分应判定为匹配");
        assertTrue(r.isRecommended());
        assertEquals("一般推荐：基本符合要求", r.getRecommendation());
        assertTrue(r.getMatchReasons().stream().anyMatch(s -> s.contains("部分掌握必需技能：2/4")),
                "应记录部分技能匹配原因: " + r.getMatchReasons());
    }

    @Test
    void mismatchesAreRecordedAndNotRecommended() {
        // 经验不足、无必需技能、学历不足、薪资超上限、无管理经验 → 0 分
        Employee e = employee(1, "大专", 50000, 60, false, "Photoshop");
        MatchingResult r = fire(e, seniorJavaPosition());

        assertEquals(0.0, r.getMatchScore(), 0.001, "无任何命中时应为 0 分（规则不设扣分）");
        assertFalse(r.isMatch());
        assertFalse(r.isRecommended());
        assertEquals("不推荐：匹配度较低", r.getRecommendation());
        assertTrue(r.getMismatchReasons().stream().anyMatch(s -> s.contains("工作经验不符合")),
                "应记录经验不匹配: " + r.getMismatchReasons());
        assertTrue(r.getMismatchReasons().stream().anyMatch(s -> s.contains("缺少所有必需技能")),
                "应记录技能缺失: " + r.getMismatchReasons());
        assertTrue(r.getMismatchReasons().stream().anyMatch(s -> s.contains("学历不符合")),
                "应记录学历不匹配: " + r.getMismatchReasons());
        assertTrue(r.getMismatchReasons().stream().anyMatch(s -> s.contains("薪资期望过高")),
                "应记录薪资过高: " + r.getMismatchReasons());
        assertTrue(r.getMismatchReasons().stream().anyMatch(s -> s.contains("缺少管理经验")),
                "应记录管理经验缺失: " + r.getMismatchReasons());
    }

    @Test
    void highPerformanceBonusApplies() {
        Position p = new Position();
        p.setTitle("中级工程师");
        p.setRequiredSkills(Arrays.asList("Java"));
        p.setPreferredSkills(Arrays.asList("Docker"));
        p.setMinExperience(2);
        p.setMaxExperience(8);
        p.setMinEducation("本科");
        p.setMinSalary(15000);
        p.setMaxSalary(30000);
        p.setLevel("中级");
        p.setRequiresManagement(false);

        // 经验 20 + 必需技能 30 + 学历 15 + 薪资 10 + 级别 5 = 80；绩效 90 再 +5 = 85
        Employee e = employee(4, "本科", 20000, 90, false, "Java");
        MatchingResult r = fire(e, p);

        assertEquals(85.0, r.getMatchScore(), 0.001);
        assertTrue(r.getMatchReasons().stream().anyMatch(s -> s.contains("绩效优秀")),
                "应记录高绩效加分原因: " + r.getMatchReasons());
        assertEquals("强烈推荐：匹配度很高", r.getRecommendation());
    }
}
