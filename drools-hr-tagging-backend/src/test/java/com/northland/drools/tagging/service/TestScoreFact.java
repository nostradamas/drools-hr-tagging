package com.northland.drools.tagging.service;

/**
 * DynamicRuleServiceTest 专用的测试事实：分数 + 命中后被规则写入的等级。
 * 独立顶层类（不做嵌套类），保证 Drools 编译器能按全限定名解析。
 */
public class TestScoreFact {
    private int score;
    private String level;

    public TestScoreFact(int score) {
        this.score = score;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}
