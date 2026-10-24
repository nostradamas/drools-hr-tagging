package com.northland.drools.tagging.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.northland.drools.mapper.TagRuleMapper;
import com.northland.drools.tagging.model.TagRule;
import com.northland.drools.tagging.model.dto.RuleValidateResult;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态规则服务（#16 重构）。
 *
 * <p>单一事实源是数据库 {@code tag_rule} 表（经 {@link TagRuleMapper} 读写）：
 * 服务启动时把全部规则加载进内存缓存，增删改先落库、成功后才更新缓存并重编译；
 * 内存缓存只是 DB 的只读镜像，不再是规则的唯一住所，重启不丢规则。</p>
 *
 * <p>热加载：重编译时用显式递增的 ReleaseId 构建新 KieModule，编译成功才原子替换
 * 当前 KieContainer 并 dispose 旧容器；编译失败保留旧容器、明确返回 false，
 * 不让一条坏规则污染正在服务的会话。</p>
 */
@Slf4j
@Service
public class DynamicRuleService {

    private final TagRuleMapper tagRuleMapper;
    private final RuleTemplateService ruleTemplateService;
    private final TagLibraryService tagLibraryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 规则缓存：DB 的内存镜像（key = ruleCode）
    private final Map<String, TagRule> ruleRepository = new ConcurrentHashMap<String, TagRule>();

    // KieContainer 缓存（volatile 保证重编译后的原子替换对读线程可见）
    private volatile KieContainer kieContainer;

    // 动态规则模块版本号，每次成功重编译递增，生成显式 ReleaseId
    private final AtomicLong moduleVersion = new AtomicLong(0);

    @Autowired
    public DynamicRuleService(TagRuleMapper tagRuleMapper,
                              RuleTemplateService ruleTemplateService,
                              TagLibraryService tagLibraryService) {
        this.tagRuleMapper = tagRuleMapper;
        this.ruleTemplateService = ruleTemplateService;
        this.tagLibraryService = tagLibraryService;
    }

    /**
     * 启动时从 DB 加载全部规则并编译。DB 不可用时降级为空缓存启动并打 WARN，
     * 不阻断应用启动；后续 createKieSession 会再次触发编译尝试。
     */
    @PostConstruct
    public synchronized void loadRulesFromDatabase() {
        ruleRepository.clear();
        try {
            List<com.northland.drools.entity.TagRule> rows = tagRuleMapper.findAllRules();
            if (rows != null) {
                for (com.northland.drools.entity.TagRule row : rows) {
                    TagRule model = toModel(row);
                    ruleRepository.put(model.getRuleCode(), model);
                }
            }
            log.info("已从数据库加载 {} 条动态规则", ruleRepository.size());
        } catch (Exception e) {
            log.warn("从数据库加载动态规则失败，以空缓存启动: {}", e.getMessage());
        }
        recompileRules();
    }

    /**
     * 添加或更新规则：生成内容 → 真编译校验 → 落库 → 更新缓存 → 重编译。
     * 任一步失败返回 false；校验不过的规则不会落库。
     */
    public synchronized boolean addOrUpdateRule(TagRule tagRule) {
        try {
            // 生成规则内容
            if (tagRule.getRuleContent() == null || tagRule.getRuleContent().trim().isEmpty()) {
                String generatedContent = ruleTemplateService.generateRuleContent(tagRule);
                tagRule.setRuleContent(generatedContent);
            }

            // 真编译校验（#14 起 validateRuleContent 走 KieBuilder 真编译）
            RuleValidateResult validation = ruleTemplateService.validateRuleContent(tagRule.getRuleContent());
            if (!validation.isValid()) {
                log.warn("规则 {} 校验未通过，拒绝保存: {}", tagRule.getRuleCode(), validation.getErrorMessage());
                return false;
            }

            // 落库（新增或按 ruleCode 更新）
            persistRule(tagRule);

            // 更新缓存并重编译；重编译失败时保留旧容器，缓存回滚到落库前状态由下次加载纠正
            ruleRepository.put(tagRule.getRuleCode(), tagRule);
            return recompileRules();

        } catch (Exception e) {
            log.error("添加/更新规则 {} 失败", tagRule.getRuleCode(), e);
            return false;
        }
    }

    /**
     * 删除规则：先落库删除，再更新缓存并重编译。
     */
    public synchronized boolean deleteRule(String ruleCode) {
        try {
            com.northland.drools.entity.TagRule existing = tagRuleMapper.findByRuleCode(ruleCode);
            if (existing == null && !ruleRepository.containsKey(ruleCode)) {
                return false;
            }
            if (existing != null) {
                tagRuleMapper.deleteById(existing.getId());
            }
            ruleRepository.remove(ruleCode);
            return recompileRules();
        } catch (Exception e) {
            log.error("删除规则 {} 失败", ruleCode, e);
            return false;
        }
    }

    /**
     * 启用/禁用规则：落库后更新缓存并重编译。
     */
    public synchronized boolean toggleRule(String ruleCode, boolean active) {
        TagRule rule = ruleRepository.get(ruleCode);
        if (rule == null) {
            return false;
        }
        try {
            com.northland.drools.entity.TagRule existing = tagRuleMapper.findByRuleCode(ruleCode);
            if (existing != null) {
                existing.setIsActive(active);
                tagRuleMapper.updateById(existing);
            }
            rule.setActive(active);
            return recompileRules();
        } catch (Exception e) {
            log.error("启停规则 {} 失败", ruleCode, e);
            return false;
        }
    }

    /**
     * 获取规则
     */
    public TagRule getRule(String ruleCode) {
        return ruleRepository.get(ruleCode);
    }

    /**
     * 获取所有规则
     */
    public List<TagRule> getAllRules() {
        return new ArrayList<TagRule>(ruleRepository.values());
    }

    /**
     * 根据标签获取规则
     */
    public List<TagRule> getRulesByTag(String tagCode) {
        List<TagRule> result = new ArrayList<TagRule>();
        for (TagRule rule : ruleRepository.values()) {
            if (tagCode.equals(rule.getTagCode())) {
                result.add(rule);
            }
        }
        return result;
    }

    /**
     * 重新编译规则引擎：显式 ReleaseId 构建新模块，成功才替换容器。
     *
     * @return 编译并替换成功返回 true；编译失败保留旧容器并返回 false
     */
    private synchronized boolean recompileRules() {
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            long version = moduleVersion.incrementAndGet();
            ReleaseId releaseId = kieServices.newReleaseId(
                    "com.northland", "dynamic-rules", "1.0." + version);
            kieFileSystem.generateAndWritePomXML(releaseId);

            // 添加所有活跃的规则
            int activeCount = 0;
            for (TagRule rule : ruleRepository.values()) {
                if (rule.isActive()) {
                    String resourcePath = "src/main/resources/rules/dynamic/" + rule.getRuleCode() + ".drl";
                    kieFileSystem.write(resourcePath, rule.getRuleContent());
                    activeCount++;
                }
            }

            // 编译规则
            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();

            Results results = kieBuilder.getResults();
            if (results.hasMessages(Message.Level.ERROR)) {
                for (Message message : results.getMessages(Message.Level.ERROR)) {
                    log.error("动态规则编译错误: {}", message.getText());
                }
                return false;
            }

            // 原子替换容器，旧容器随即释放
            KieContainer newContainer = kieServices.newKieContainer(releaseId);
            KieContainer oldContainer = this.kieContainer;
            this.kieContainer = newContainer;
            if (oldContainer != null) {
                oldContainer.dispose();
            }

            log.info("动态规则引擎重新编译成功（版本 1.0.{}），共 {} 条规则，其中 {} 条启用",
                    version, ruleRepository.size(), activeCount);
            return true;

        } catch (Exception e) {
            log.error("重新编译动态规则引擎失败，保留现有容器", e);
            return false;
        }
    }

    /**
     * 创建新的KieSession
     */
    public KieSession createKieSession() {
        if (kieContainer == null) {
            recompileRules();
        }

        if (kieContainer != null) {
            KieSession session = kieContainer.newKieSession();
            try {
                session.setGlobal("tagLibraryService", tagLibraryService);
            } catch (RuntimeException e) {
                // 当前规则集未声明该 global（如全部规则被删除/停用后的空规则集）时忽略，
                // 没有规则会用到它；声明了的规则集会正常注入。
                log.debug("规则集未声明 tagLibraryService global，跳过注入: {}", e.getMessage());
            }
            return session;
        }

        return null;
    }

    /**
     * 测试规则
     */
    public Map<String, Object> testRule(String ruleCode, Object testData) {
        Map<String, Object> result = new HashMap<String, Object>();

        KieSession session = null;
        try {
            TagRule rule = ruleRepository.get(ruleCode);
            if (rule == null) {
                result.put("success", false);
                result.put("message", "规则不存在");
                return result;
            }

            session = createKieSession();
            if (session == null) {
                result.put("success", false);
                result.put("message", "创建规则会话失败");
                return result;
            }

            // 插入测试数据
            session.insert(testData);

            // 执行规则
            int firedRules = session.fireAllRules();

            result.put("success", true);
            result.put("firedRules", firedRules);
            result.put("message", "规则测试完成，触发了 " + firedRules + " 条规则");

            // 更新执行统计（内存 + DB，DB 失败不影响测试结果）
            rule.incrementExecutionCount();
            try {
                tagRuleMapper.updateExecutionCount(ruleCode);
            } catch (Exception e) {
                log.warn("更新规则 {} 执行统计失败: {}", ruleCode, e.getMessage());
            }

        } catch (Exception e) {
            log.error("测试规则 {} 失败", ruleCode, e);
            result.put("success", false);
            result.put("message", "规则测试失败: " + e.getMessage());
        } finally {
            if (session != null) {
                session.dispose();
            }
        }

        return result;
    }

    /**
     * 获取规则统计信息
     */
    public Map<String, Object> getRuleStatistics() {
        Map<String, Object> stats = new HashMap<String, Object>();

        long totalRules = ruleRepository.size();
        long activeRules = 0;
        for (TagRule rule : ruleRepository.values()) {
            if (rule.isActive()) {
                activeRules++;
            }
        }
        long inactiveRules = totalRules - activeRules;

        stats.put("totalRules", totalRules);
        stats.put("activeRules", activeRules);
        stats.put("inactiveRules", inactiveRules);

        // 按模板分组统计
        Map<String, Long> templateStats = new HashMap<String, Long>();
        for (TagRule rule : ruleRepository.values()) {
            String template = rule.getRuleTemplate();
            templateStats.put(template, templateStats.getOrDefault(template, 0L) + 1);
        }
        stats.put("templateStats", templateStats);

        // 执行次数统计
        long totalExecutions = 0;
        for (TagRule rule : ruleRepository.values()) {
            totalExecutions += rule.getExecutionCount();
        }
        stats.put("totalExecutions", totalExecutions);

        return stats;
    }

    /**
     * 初始化默认规则。仅当规则库为空时写入，避免每次重启覆盖已落库的修改。
     */
    public void initializeDefaultRules() {
        if (!ruleRepository.isEmpty()) {
            log.info("规则库已有 {} 条规则，跳过默认规则初始化", ruleRepository.size());
            return;
        }
        // 技术专家规则
        TagRule techExpertRule = new TagRule("RULE_TECH_EXPERT", "技术专家规则", "TECH_EXPERT", "COMPOSITE");
        techExpertRule.setDescription("基于工作经验、技能数量和技能等级判断技术专家");
        techExpertRule.setConditions("{\"minYears\": 8, \"minSkills\": 5, \"avgSkillLevel\": 4}");
        techExpertRule.setPriority(9);
        addOrUpdateRule(techExpertRule);

        // 高绩效规则
        TagRule highPerformerRule = new TagRule("RULE_HIGH_PERFORMER", "高绩效规则", "HIGH_PERFORMER", "PERFORMANCE_BASED");
        highPerformerRule.setDescription("基于绩效评分判断高绩效员工");
        highPerformerRule.setConditions("{\"minScore\": 85, \"operator\": \">=\"}");
        highPerformerRule.setPriority(8);
        addOrUpdateRule(highPerformerRule);

        // 博士学历规则
        TagRule phdRule = new TagRule("RULE_PHD_DEGREE", "博士学历规则", "PHD_DEGREE", "EDUCATION_BASED");
        phdRule.setDescription("基于学历判断博士学位");
        phdRule.setConditions("{\"educationLevels\": [\"博士\"], \"matchType\": \"ANY\"}");
        phdRule.setPriority(7);
        addOrUpdateRule(phdRule);

        // 强领导力规则
        TagRule leadershipRule = new TagRule("RULE_STRONG_LEADER", "强领导力规则", "STRONG_LEADER", "LEADERSHIP_BASED");
        leadershipRule.setDescription("基于领导力评分、管理经验和团队规模判断强领导力");
        leadershipRule.setConditions("{\"minLeadershipScore\": 85, \"minManagementYears\": 3, \"minTeamSize\": 5}");
        leadershipRule.setPriority(9);
        addOrUpdateRule(leadershipRule);

        // 全栈开发者规则
        TagRule fullstackRule = new TagRule("RULE_FULLSTACK_DEV", "全栈开发者规则", "FULLSTACK_DEV", "SKILL_BASED");
        fullstackRule.setDescription("基于前后端技能判断全栈开发者");
        fullstackRule.setConditions("{\"requiredSkills\": [\"Java\", \"JavaScript\", \"数据库\"], \"matchType\": \"ALL\"}");
        fullstackRule.setPriority(8);
        addOrUpdateRule(fullstackRule);

        log.info("默认规则初始化完成");
    }

    // ------------------------------------------------------------------
    // 落库与模型转换：tagging.model.TagRule（工作内存/接口层）
    // 与 entity.TagRule（tag_rule 表）字段同名同义，此处做显式双向转换。
    // ------------------------------------------------------------------

    private void persistRule(TagRule model) {
        com.northland.drools.entity.TagRule existing = tagRuleMapper.findByRuleCode(model.getRuleCode());
        com.northland.drools.entity.TagRule entity = toEntity(model);
        if (existing == null) {
            tagRuleMapper.insert(entity);
            model.setId(entity.getId());
        } else {
            entity.setId(existing.getId());
            tagRuleMapper.updateById(entity);
            model.setId(existing.getId());
        }
    }

    private com.northland.drools.entity.TagRule toEntity(TagRule model) {
        com.northland.drools.entity.TagRule entity = new com.northland.drools.entity.TagRule();
        entity.setId(model.getId());
        entity.setRuleCode(model.getRuleCode());
        entity.setRuleName(model.getRuleName());
        entity.setTagCode(model.getTagCode());
        entity.setRuleContent(model.getRuleContent());
        entity.setRuleTemplate(model.getRuleTemplate());
        entity.setConditions(model.getConditions());
        entity.setDescription(model.getDescription());
        entity.setPriority(model.getPriority());
        entity.setIsActive(model.isActive());
        entity.setVersion(model.getVersion());
        entity.setCreatedBy(model.getCreatedBy());
        entity.setUpdatedBy(model.getUpdatedBy());
        entity.setCreatedTime(model.getCreatedTime());
        entity.setUpdatedTime(model.getUpdatedTime());
        entity.setLastExecutedTime(model.getLastExecutedTime());
        entity.setExecutionCount(model.getExecutionCount());
        entity.setTestCases(toJson(model.getTestCases()));
        return entity;
    }

    private TagRule toModel(com.northland.drools.entity.TagRule entity) {
        TagRule model = new TagRule();
        model.setId(entity.getId());
        model.setRuleCode(entity.getRuleCode());
        model.setRuleName(entity.getRuleName());
        model.setTagCode(entity.getTagCode());
        model.setRuleContent(entity.getRuleContent());
        model.setRuleTemplate(entity.getRuleTemplate());
        model.setConditions(entity.getConditions());
        model.setDescription(entity.getDescription());
        model.setPriority(entity.getPriority() == null ? 5 : entity.getPriority());
        model.setActive(entity.getIsActive() == null || entity.getIsActive());
        model.setVersion(entity.getVersion());
        model.setCreatedBy(entity.getCreatedBy());
        model.setUpdatedBy(entity.getUpdatedBy());
        model.setCreatedTime(entity.getCreatedTime());
        model.setUpdatedTime(entity.getUpdatedTime());
        model.setLastExecutedTime(entity.getLastExecutedTime());
        model.setExecutionCount(entity.getExecutionCount() == null ? 0L : entity.getExecutionCount());
        model.setTestCases(fromJson(entity.getTestCases()));
        return model;
    }

    private String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            log.warn("序列化 testCases 失败: {}", e.getMessage());
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("反序列化 testCases 失败: {}", e.getMessage());
            return new ArrayList<String>();
        }
    }
}
