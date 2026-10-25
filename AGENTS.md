# AGENTS.md — Drools Rules Engine 项目级 AI 说明

## 项目定位
开源规则引擎管理系统，基于 Drools 7.73 + Spring Boot 2.7 实现员工智能标签、岗位匹配与人才分析。

## 目录结构
```
/
├── pom.xml                     # 根 POM（多模块）
├── drools-hr-tagging-backend/  # 后端 Maven 子模块
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/northland/drools/
│       │   ├── DroolsApplication.java
│       │   ├── common/          # 公共层（config/exception/response）
│       │   ├── entity/          # MyBatis-Plus 实体
│       │   ├── mapper/          # MyBatis Mapper
│       │   ├── matching/        # 岗位匹配（controller/model/service）
│       │   └── tagging/         # 标签系统（controller/model/service）
│       ├── main/resources/
│       │   ├── application.properties
│       │   ├── mapper/          # Mapper XML（可选/预留）
│       │   └── rules/           # DRL 规则文件
│       └── test/
├── drools-hr-tagging-frontend/  # Vue 2 前端
│   ├── package.json
│   └── src/
├── database/                   # SQL 脚本
│   └── create_tables.sql
├── docs/                       # 教程与图片（建设中）
├── specs/                      # Spec 规格说明
│   ├── requirements.md
│   ├── design.md
│   ├── tasks.md
│   └── acceptance.md
├── .github/                    # CI + Issue 模板
├── README.md                   # 中英双语
├── LICENSE                     # Apache-2.0
├── CONTRIBUTING.md
└── CHANGELOG.md
```

## 技术栈硬性约束
- Java 1.8 目标（release=8 编译）；**运行时限 JDK 8 ~ 17**：Drools 7.x 的 MVEL 依赖 java.lang.Compiler，JDK 21+ 被移除会 ClassNotFoundException
- Spring Boot 2.7.18（2.7.x 最后补丁版；不升级 3.x，因 Drools 7.x 对 Jakarta EE 支持有限）
- Drools 7.73.0.Final（不升级 8.x，保持 javax.* 命名空间兼容）
- MyBatis-Plus 3.5.3
- MySQL 5.7+（开发可用 H2 内存库替代）
- Vue 2.6 + Element UI（不升级 Vue 3）

## 构建与测试命令
```bash
# 编译后端
mvn clean compile -f drools-hr-tagging-backend/pom.xml

# 运行测试
mvn clean test -f drools-hr-tagging-backend/pom.xml

# 打包
mvn clean package -f pom.xml

# 启动后端（需本地 MySQL）
mvn spring-boot:run -f drools-hr-tagging-backend/pom.xml

# 启动前端
cd drools-hr-tagging-frontend && npm install && npm run dev
```

## 架构分层与依赖方向
```
Controller → Service → (Drools KieSession + MyBatis Mapper) → Entity/Model
```
- Controller 层不直接操作 KieSession
- Service 层负责 KieSession 创建/销毁和事务管理
- Model 层分两类：`entity/*`（DB 持久化）和 `tagging/model/*`（Drools 工作内存对象）
- DRL 规则只引用 `tagging/model/*` 和 `matching/model/*`

## 关键设计约束（不可违反）
1. **切勿为 Drools 规则变量使用 `matching.model.Employee`以外的同名实体**
   - `matching/model/Employee.java` ≠ `entity/Employee.java`，DRL 只认 matching 包的
2. **KieSession 必须 dispose** — 每次用完调用 `session.dispose()`，否则内存泄漏
3. **动态规则用 KieFileSystem 编译** — 不用 `kie-spring` XML 方式
4. **KieSession 每请求新建** — IoC 只暴露 KieContainer 单例，session 用完必须 dispose
5. **标签过期策略**：考评 6 月、能力 1 年、履历 2 年
6. **匹配分数累加制**：基础 0 分，各项规则累加，上限 100，不设扣分

## 模块说明（backup 迁移整合）
- `tagging/controller/EmployeeController` 等员工 CRUD —— 来自旧 test 后端，API 前缀 `/api/employees`
- `tagging/service/RuleService/Impl` + `model/dto/RuleExecuteResult|RuleValidateResult` —— 规则模板验证/编译/执行
- `entity/RuleTemplate`（rule_template 表）与 `tagging/model/RuleTemplate`（枚举）**同名不同物**，前者是 MyBatis 实体
- 前端未串接后端的 Mock 页面（人才洞察等）已按「保持核心 Drools 功能示例」原则移除

## 已知陷阱
- **本机 JDK 26 编译需注意**：根 POM 有 jdk-annotation-processing profile（JDK 23+ 自动启用 -proc:full 跑 Lombok）；跑测试/启动应用请切 JDK 8~17（export JAVA_HOME=...）
- `RuleTemplateService` 的 `generateCompositeRule` 当前为骨架代码，COMPOSITE 模板未完全实现
- `employee-tagging-rules.drl` 中的 `skills` 字段引用的是 `matching.model.Employee` 的 `List<String> skills`，不是 `entity.Employee` 的 `technicalSkills`
