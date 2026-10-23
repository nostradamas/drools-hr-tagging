package com.northland.drools.tagging.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.northland.drools.entity.EmployeeTag;
import com.northland.drools.tagging.model.EnhancedEmployee;
import com.northland.drools.tagging.model.TagCategory;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * persistRuleEngineTags 落库逻辑验证：过滤条件、字段映射、旧标签失效顺序。
 * 不起 Spring 上下文，spy 掉 MyBatis-Plus 基类的 update/saveBatch。
 */
class EmployeeTagServiceImplTest {

    private EmployeeTagServiceImpl service;
    private EnhancedEmployee employee;

    @BeforeAll
    static void initEntityMetadata() {
        // 无 Spring 上下文时手动注册 entity 元数据，LambdaUpdateWrapper 解析列名依赖它
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), EmployeeTag.class);
    }

    @BeforeEach
    void setUp() {
        service = spy(new EmployeeTagServiceImpl());
        doReturn(true).when(service).update(any(Wrapper.class));
        doReturn(true).when(service).saveBatch(anyCollection());

        employee = new EnhancedEmployee();
        employee.setId(7L);
        employee.setName("落库测试员工");
    }

    private com.northland.drools.tagging.model.EmployeeTag modelTag(
            String code, String source, boolean active, LocalDateTime expireTime) {
        com.northland.drools.tagging.model.EmployeeTag t = new com.northland.drools.tagging.model.EmployeeTag();
        t.setTagCode(code);
        t.setTagName("标签-" + code);
        t.setCategory(TagCategory.EXPERIENCE_LEVEL);
        t.setDimension("EXPERIENCE");
        t.setConfidence(0.8);
        t.setSource(source);
        t.setReason("测试打标原因");
        t.setExpireTime(expireTime);
        t.setActive(active);
        return t;
    }

    @Test
    void nullEmployeeReturnsZero() {
        assertEquals(0, service.persistRuleEngineTags(null));
        verify(service, never()).update(any(Wrapper.class));
        verify(service, never()).saveBatch(anyCollection());
    }

    @Test
    void noEmployeeIdSkipsPersistence() {
        employee.setId(null);
        employee.getCurrentTags().add(modelTag(
                "SENIOR_5TO10", "RULE_ENGINE", true, LocalDateTime.now().plusYears(2)));
        assertEquals(0, service.persistRuleEngineTags(employee));
        verify(service, never()).update(any(Wrapper.class));
        verify(service, never()).saveBatch(anyCollection());
    }

    @Test
    void persistsOnlyActiveRuleEngineTagsWithFieldMapping() {
        employee.getCurrentTags().add(modelTag(
                "SENIOR_5TO10", "RULE_ENGINE", true, LocalDateTime.now().plusYears(2)));
        employee.getCurrentTags().add(modelTag(
                "MID_3TO5", "RULE_ENGINE", true, LocalDateTime.now().minusDays(1))); // 已过期
        employee.getCurrentTags().add(modelTag(
                "FRESH_GRAD", "RULE_ENGINE", false, LocalDateTime.now().plusYears(2))); // 非活跃
        employee.getCurrentTags().add(modelTag(
                "MANUAL_TAG", "MANUAL", true, LocalDateTime.now().plusYears(2))); // 手动来源

        int persisted = service.persistRuleEngineTags(employee);

        assertEquals(1, persisted, "只有 RULE_ENGINE + 活跃 + 未过期的标签应落库");
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(service).saveBatch(captor.capture());
        assertEquals(1, captor.getValue().size());
        EmployeeTag saved = (EmployeeTag) captor.getValue().iterator().next();
        assertEquals(Long.valueOf(7L), saved.getEmployeeId());
        assertEquals("SENIOR_5TO10", saved.getTagCode());
        assertEquals("标签-SENIOR_5TO10", saved.getTagName());
        assertEquals("EXPERIENCE_LEVEL", saved.getCategory());
        assertEquals("EXPERIENCE", saved.getDimension());
        assertEquals(Double.valueOf(0.8), saved.getConfidence());
        assertEquals("RULE_ENGINE", saved.getSource());
        assertEquals("测试打标原因", saved.getReason());
        assertNotNull(saved.getExpireTime());
        assertEquals(Boolean.TRUE, saved.getIsActive());
    }

    @Test
    void deactivatesOldTagsWithEmployeeAndSourceBeforeSave() {
        employee.getCurrentTags().add(modelTag(
                "SENIOR_5TO10", "RULE_ENGINE", true, LocalDateTime.now().plusYears(2)));

        assertEquals(1, service.persistRuleEngineTags(employee));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<EmployeeTag>> wrapperCaptor =
                ArgumentCaptor.forClass((Class) LambdaUpdateWrapper.class);
        verify(service).update(wrapperCaptor.capture());
        LambdaUpdateWrapper<EmployeeTag> wrapper = wrapperCaptor.getValue();
        // eq 的参数在 getSqlSegment() 惰性格式化时才进入 paramNameValuePairs，必须先取 SQL 段
        String segment = wrapper.getSqlSegment();
        assertTrue(segment.contains("employee_id"), "失效条件应限定该员工: " + segment);
        assertTrue(segment.contains("source"), "失效条件应限定规则来源: " + segment);
        assertTrue(wrapper.getSqlSet().contains("is_active"), "应更新 is_active 列: " + wrapper.getSqlSet());
        Collection<Object> params = wrapper.getParamNameValuePairs().values();
        assertTrue(params.contains(7L), "失效条件应绑定员工ID: " + params);
        assertTrue(params.contains("RULE_ENGINE"), "失效条件应绑定规则来源: " + params);
        assertTrue(params.contains(false), "应将旧标签置为 inactive: " + params);

        InOrder inOrder = inOrder(service);
        inOrder.verify(service).update(any(Wrapper.class));
        inOrder.verify(service).saveBatch(anyCollection());
    }

    @Test
    void noRuleEngineTagsSkipsBothOperations() {
        employee.getCurrentTags().add(modelTag(
                "MANUAL_TAG", "MANUAL", true, LocalDateTime.now().plusYears(2)));
        assertEquals(0, service.persistRuleEngineTags(employee));
        verify(service, never()).update(any(Wrapper.class));
        verify(service, never()).saveBatch(anyCollection());
    }
}
