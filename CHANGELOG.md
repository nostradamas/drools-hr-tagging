# 更新日志 / Changelog

所有重要的变更都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

---

## [Unreleased]

### Fixed
- 岗位匹配 DRL 两处缺陷（#18，新增测试时发现）：函数签名使用 `List<String>` 泛型导致 Drools 7.73 无法解析、整个岗位匹配规则编译失败（连带 DroolsConfig 启动即抛错），改无泛型签名修复；推荐定档规则因评分改分不 update 事实而只按初始 0 分求值、恒为「不推荐」，规则按 agenda-group 拆分评分/推荐两阶段并在服务层分阶段触发后修复
- 动态规则落库与真热加载（#16）：`DynamicRuleService` 改以 `tag_rule` 表为单一事实源（启动从 DB 加载、增删改先落库后重编译，重启不丢）；重编译改用显式递增 ReleaseId 构建并原子替换 KieContainer，编译失败保留旧容器；`RuleServiceImpl.compileAndLoadRule` 不再编译即丢弃，改为委托动态规则通道真正加载；该类 `System.out/err` 全部改为 slf4j 日志；默认规则仅在规则库为空时初始化
- 规则管理安全与模板引擎校验（#14）：规则管理写接口（POST/PUT/DELETE `/api/rule-management/**`）新增 Spring Security 管理员认证（HTTP Basic，账号走环境变量 `ADMIN_USERNAME` / `ADMIN_PASSWORD`），CORS 由通配 `*`+凭据收紧为配置白名单（`CORS_ALLOWED_ORIGINS`）；`RuleTemplateService.validateRule` 由字符串包含检查改为 KieBuilder 真实编译校验；复合模板在无有效条件时拒绝生成空 when 规则；条件中的技能/学历数组改为类型安全解析，修复 `(String[])` 强转的 ClassCastException，并对拼入 DRL 的字符串做转义、运算符加白名单
- 打标规则三重缺陷：静态 DRL 类型不匹配导致规则静默不生效、档位区间重叠产生矛盾标签、双文件重复打标（#10）
- 规则产出标签此前仅 println，现由服务层统一批量落库 employee_tag 表（演示场景无主键时仅内存返回）（#10）

### Added
- GitHub Actions CI（#18）：push / PR 触发，JDK 8 与 17 双矩阵执行 `mvn clean test` 作为合并门禁；README 增加 CI 状态徽章
- 岗位匹配规则测试 4 项：全维度命中封顶 100、必需技能部分匹配按比例计分、不匹配原因留痕、高绩效加分（#18）
- 动态规则服务测试 5 项：落库命中、重建实例恢复、坏规则拒绝、删除与停用（#16）
- 打标规则测试套件：DRL 可编译性、经验/绩效应互斥、标签来源与去重共 5 项断言（#10）
- 服务层落库测试 5 项：persistRuleEngineTags 过滤条件、entity 字段映射、旧标签失效 SQL 条件与先失效后保存顺序（#12）

### Changed
- Spring Boot 2.7.0 → 2.7.18（2.7.x 最后补丁版，含 Spring Framework 5.3.31 等依赖刷新）（#18）
- 规则重写为互斥区间档位：经验 5 档 / 绩效 5 档，双文件按履历/考评维度分工（#10）

## [1.0.0] - 2026-10-23

基于 Drools 7.73 + Spring Boot 2.7 的员工智能打标与岗位匹配系统：规则在线管理与动态热加载、多维自动打标、五维岗位匹配评分，配套 Vue 2 管理前端。下方为本版本的功能清单与发布前框架审查修复记录。

### 新增 / Added

#### 规则管理 (Rule Management)
- 规则在线 CRUD、启用/停用、批量操作、导入导出
- 7 种规则模板引擎（经验/技能/绩效/学历/领导力/复合/自定义），条件配置表单生成 DRL，支持在线预览与语法校验
- 动态规则编译与热加载（KieFileSystem + KieContainer 刷新）
- 规则执行统计与默认规则一键初始化

#### 员工自动打标 (Employee Auto-Tagging)
- 基于 Drools 的多维度自动打标：能力（技能/领导力/沟通/创新）、履历（年限/学历/行业/项目）、考评（绩效/潜力/协作/趋势）
- 标签生命周期管理：按维度自动过期（考评 6 个月 / 能力 1 年 / 履历 2 年）、置信度评估、来源追溯（规则引擎/手动/系统）
- 批量打标与标签统计分析、培训推荐

#### 岗位匹配 (Position Matching)
- 五维评分规则（技能/经验/学历/薪资期望/管理经验），0-100 分制
- 单人单岗、单人多岗、多人单岗、批量匹配四类 API，按匹配分排序与推荐筛选

#### 数据与接口 (Data & API)
- 7 张表建库脚本（含演示数据）：employee / tag / employee_tag / tag_rule / tag_category / rule_template / tag_change_history
- 员工、标签、标签分类的 MyBatis-Plus 分页 CRUD
- 统一响应封套 ApiResponse 与全局异常处理

#### 前端 (Frontend)
- Vue 2.6 + Element UI 管理界面，5 个功能模块：仪表盘 / 标签库管理 / 规则管理 / 员工标签 / 统计分析
- 全部页面与后端真实 API 串通（无 Mock 数据）

#### 工程基础 (Engineering)
- Maven 多模块结构，编译目标 JDK 8（`release=8` 编译期 API 边界校验）
- Apache-2.0 LICENSE、中英双语 README、CONTRIBUTING、Issue 模板、specs 规格文档（需求/设计/任务/验收）

### 修复 / Fixed（发布前框架审查，#1~#8）
- DRL 引用不存在的实体属性导致启动失败，更正为 `workExperience`（#1）
- KieSession 生命周期重构：仅暴露 KieContainer 单例，session 每请求新建 + dispose（#2）
- CORS 通配符源与凭据模式冲突改为 `addAllowedOriginPattern`（#3）
- MyBatis-Plus lambda 引用不存在属性导致标签查询崩溃（#4）
- JDK9+ API 误用替换为 JDK8 API；建表 SQL 括号错配修复（#5）
- 响应封套统一、冗余端点与死代码清理（#6）
- README/CHANGELOG 与代码全面对齐，`vue.config.js` 补 9528 端口配置（#8）
