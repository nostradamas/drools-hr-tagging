# 任务规格说明书

## Phase 1: 基础设施（已完成）
- [x] 项目多模块 Maven 结构搭建
- [x] 包名重构：com.northland.drools
- [x] LICENSE: Apache-2.0
- [x] README: 中英双语
- [x] .gitignore / .editorconfig
- [x] CONTRIBUTING.md / CHANGELOG.md
- [x] CI 流水线配置

## Phase 2: 实体去重与模型统一（已完成）
- [x] 统一 entity 包（DB 持久化层）
- [x] 保持 tagging.model 包（Drools 工作内存层）
- [x] 确认双包没有字段不一致的问题

## Phase 3: 规则引擎增强
- [ ] CUSTOM 模板完整实现
- [ ] 规则测试框架
- [ ] 规则版本对比

## Phase 4: 测试覆盖
- [ ] Service 层单元测试（JUnit 5 + Mockito）
- [ ] Controller 层集成测试（MockMvc）
- [ ] Drools 规则单元测试
- [ ] 测试覆盖率 ≥ 70%

## Phase 5: 发布准备
- [ ] v0.1.0 release
- [ ] Maven Central 发布配置
- [ ] Docker 支持
- [ ] 文档站点

## Phase 6: 社区运营
- [ ] 发布到 V2EX / 掘金
- [ ] Roadmap issue 模板
- [ ] Issue/PR 响应时效 SLA
