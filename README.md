# Drools HR Tagging — Drools 规则引擎实战示例

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Drools-7.73.0-blue" alt="Drools"/>
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.3-orange" alt="MyBatis-Plus"/>
  <img src="https://img.shields.io/badge/MySQL-5.7-yellow" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Vue-2.6-4FC08D" alt="Vue"/>
  <img src="https://img.shields.io/badge/Element%20UI-2.15-409EFF" alt="Element UI"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/Java-1.8-red" alt="Java"/>
  <a href="https://github.com/nostradamas/drools-hr-tagging/actions/workflows/ci.yml"><img src="https://github.com/nostradamas/drools-hr-tagging/actions/workflows/ci.yml/badge.svg" alt="CI"/></a>
</p>

---

## 项目简介 / Introduction

**Drools HR Tagging** 是一套基于 Drools 规则引擎的员工智能标签与岗位匹配系统，展示规则引擎在**真实 HR 业务场景**中的落地应用。

> 🎯 **项目定位**：不是玩具 Demo，而是完整的 Drools 规则引擎实战参考实现——从 DRL 规则编写、动态规则编译、7种规则模板引擎，到多维度员工标签打分、岗位智能匹配，全套可运行、可二次开发。

**Drools HR Tagging** is a hands-on reference implementation demonstrating how to apply the Drools rule engine in real-world HR scenarios—employee auto-tagging, position matching, talent analytics—with fully runnable code, DRL rule examples, and a visual management UI.

### 适用场景 / Use Cases
- 📋 **人才盘点** — 自动根据员工能力/履历/绩效生成多维度标签
- 🎯 **招聘匹配** — 基于技能/经验/学历/薪资的智能岗位推荐
- 📈 **培训规划** — 根据标签差距推荐个性化培训
- 🔄 **组织优化** — 分析团队标签分布与能力短板

---

## 核心功能 / Core Features

### 规则管理 / Rule Management
- **动态规则 CRUD**: 在线创建、编辑、启用/停用 Drools 规则
- **在线编辑**: 规则模板 + 条件配置表单快速生成 DRL，支持在线预览与语法校验
- **模板引擎**: 预定义规则模板，快速生成标准化规则
- **规则统计**: 规则执行次数、模板分布统计与默认规则一键初始化

### 员工标签 / Employee Tagging
- **多维度自动打标**: 基于 Drools 规则引擎，从履历（经验档位/学历）、技能、考评（绩效档位/成长/协作）、潜力等多维度自动为员工打标，档位区间互斥、同维度单命中
- **自定义标签规则**: 7 种规则模板（经验/技能/绩效/学历/领导力/复合/自定义）在线配置生成 DRL
- **标签生命周期**: 按维度自动过期（考评 6 月 / 能力 1 年 / 履历 2 年）与置信度评估；打标结果自动落库 employee_tag 表（先失效同源旧标签再批量保存，演示场景无主键时仅内存返回）

### 岗位匹配 / Position Matching
- **技能匹配**: 基于技能标签库进行精准匹配
- **经验匹配**: 工作年限与领域经验匹配
- **学历匹配**: 学历与专业要求匹配
- **综合评分**: 多维度加权评分，智能推荐最优人选

### 标签库 / Tag Library
- **可管理标签系统**: 支持标签的增删改查、分类管理
- **标签维度**: 能力(ABILITY) / 履历(EXPERIENCE) / 考评(EVALUATION) 三维度 15 分类
- **标签统计**: 维度/分类分布统计，支持导入导出

---

## 系统架构 / Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Frontend)                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Vue 2.6 + Element UI + Vue Router            │  │
│  └──────────────────────┬────────────────────────────────────┘  │
│                         │ HTTP/REST API                         │
├─────────────────────────┼───────────────────────────────────────┤
│                    后端 (Backend)                               │
│  ┌──────────────────────┴────────────────────────────────────┐  │
│  │              Spring Boot 2.7.18 (REST Controller)         │  │
│  ├───────────────────────────────────────────────────────────┤  │
│  │              Service Layer (业务逻辑层)                    │  │
│  ├──────────┬──────────────────────────────┬─────────────────┤  │
│  │  Rule    │  Tag Engine                  │  Matching       │  │
│  │ Manager  │  (标签引擎)                  │  Engine         │  │
│  │          │                              │  (匹配引擎)     │  │
│  ├──────────┴──────────────────────────────┴─────────────────┤  │
│  │              Drools Rule Engine 7.73.0                    │  │
│  ├───────────────────────────────────────────────────────────┤  │
│  │              MyBatis-Plus 3.5.3 (DAO 层)                  │  │
│  └──────────────────────┬────────────────────────────────────┘  │
│                         │ JDBC                                  │
├─────────────────────────┼───────────────────────────────────────┤
│                     数据层 (Data Layer)                         │
│  ┌──────────────────────┴────────────────────────────────────┐  │
│  │                    MySQL 5.7                               │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 快速开始 / Quick Start

### 环境要求 / Prerequisites

| 工具 / Tool        | 版本 / Version |
|-------------------|---------------|
| JDK               | 8（推荐）/ 11 ~ 17（Drools 7.x 不兼容 JDK 21+） |
| Maven             | 3.6+          |
| MySQL             | 5.7+          |
| Node.js           | 14 / 16（17+ 需加参数，见下方前端步骤） |

### 30 秒预览（免数据库）/ 30-Second Preview (No Database)

想先看规则引擎的效果，不必先装 MySQL：岗位匹配的演示端点使用代码内构造的示例员工/岗位数据，经 DRL 在内存中计算，**不读写数据库**。只启动后端即可调用：

```bash
mvn spring-boot:run -f drools-hr-tagging-backend/pom.xml

curl http://localhost:8080/api/matching/test       # 单个示例员工 × 单个岗位：五维评分与推荐档位
curl http://localhost:8080/api/matching/test-data  # 演示用的示例员工与岗位数据
curl http://localhost:8080/api/matching/demo       # 全部示例员工对「高级 Java 开发工程师」岗位的批量匹配结果
```

预期效果：返回匹配总分（0–100）、各维度得分与推荐档位（强烈推荐 / 一般推荐 / 不推荐）。

> 实现说明：连接池配置了 `initialization-fail-timeout=-1`（懒初始化），数据库不可用时应用仍可启动；此时依赖数据库的接口会在连接恢复前逐请求返回错误。若希望启动时即校验数据库，删除该配置行即可（Hikari 默认 fail-fast）。

> 其余接口（员工/标签 CRUD、打标落库、规则管理等）需要 MySQL，请按下方「Docker 一键启动」或「安装步骤」启动完整环境。

The matching demo endpoints above evaluate in-code sample data with the DRL rules in memory — no database access. All other APIs require MySQL.

### Docker 一键启动 / Docker Compose

```bash
docker compose up -d --build
```

- 自动拉起 MySQL 8 与后端：MySQL 首次启动时执行 `database/create_tables.sql` 建库建表并导入演示数据，后端等 MySQL 健康检查通过后才启动
- 后端访问 / Backend: http://localhost:8080 ；MySQL 映射到本机 3306，数据持久化在数据卷 `drools-mysql-data`
- MySQL root 密码可用环境变量 `MYSQL_ROOT_PASSWORD` 覆盖（默认 `root`），后端的数据源密码随之同步
- 管理员账号（规则管理写接口用）来自环境变量 `ADMIN_USERNAME` / `ADMIN_PASSWORD`：compose 默认用户名 `admin`；密码未设置时启动日志会打印一次性随机密码，正式使用请显式设置 `ADMIN_PASSWORD`
- 前端不在 compose 内，仍按下方安装步骤第 5 步本地启动（端口 9528）

### 安装步骤 / Installation

```bash
# 1. 克隆项目 / Clone the repository
git clone https://github.com/nostradamas/drools-hr-tagging.git
cd drools-hr-tagging

# 2. 初始化数据库 / Initialize the database (脚本自动建库建表)
mysql -u root -p < database/create_tables.sql

# 3. 修改数据库配置 / Configure the datasource
#    编辑 drools-hr-tagging-backend/src/main/resources/application.properties
#    密码默认读取环境变量 DB_PASSWORD，本机默认 root
#    Edit datasource settings; password comes from env DB_PASSWORD (default: root)

# 4. 启动后端 / Start the backend (端口 8080)
mvn spring-boot:run -f drools-hr-tagging-backend/pom.xml

# 5. 启动前端 / Start the frontend (新开终端 / new terminal, 端口 9528)
#    前端基于 Vue CLI 4（webpack 4），Node 14 / 16 可直接运行；
#    Node 17+ 与 OpenSSL 3 不兼容（启动报 error:0308010C），需先设置：
#    Node 17+ requires the legacy OpenSSL provider (webpack 4 vs OpenSSL 3):
#      export NODE_OPTIONS=--openssl-legacy-provider
cd drools-hr-tagging-frontend
npm install
npm run dev
```

访问 / Access: http://localhost:9528 (前端 / Frontend) | http://localhost:8080/api (后端 / Backend)

> 数据库默认连接 `localhost:3306/employee_tagging`，账号 root，密码取环境变量 `DB_PASSWORD`（默认 root）。

> **安全配置 / Security**：规则管理写接口（POST/PUT/DELETE `/api/rule-management/**`，含提交/编译 DRL）要求管理员认证（HTTP Basic），用户名取环境变量 `ADMIN_USERNAME`（默认 `admin`），密码取 `ADMIN_PASSWORD`（未设置时启动日志会打印一次性随机密码，生产环境务必显式设置）；其余只读/演示接口不要求认证。CORS 不再使用通配来源，允许名单取环境变量 `CORS_ALLOWED_ORIGINS`（逗号分隔，默认 `http://localhost:9528,http://localhost:8080`）。

### 运行测试 / Running Tests

```bash
# 测试必须在 JDK 8 ~ 17 下运行（Drools 7.73 的 MVEL 依赖 JDK 21 起被移除的 java.lang.Compiler）
# Tests require JDK 8 ~ 17 (MVEL relies on java.lang.Compiler, removed in JDK 21)
export JAVA_HOME=$(/usr/libexec/java_home -v 11)   # macOS 示例 / macOS example
mvn clean test -f drools-hr-tagging-backend/pom.xml
```

当前 26 个测试用例：规则层 5 例（DRL 可编译、经验/绩效档位互斥、标签来源/原因/过期时间、无重复打标）+ 服务层 5 例（落库过滤条件、字段映射、旧标签失效顺序）+ 模板服务 7 例（#14：真实编译校验、复合规则空条件拒绝、技能/学历数组条件生成）+ 动态规则 5 例（#16：落库后命中打标、重建实例从 DB 恢复、坏规则拒绝且不落库、删除/停用生效）+ 岗位匹配 4 例（#18：五维评分累加、封顶 100、部分技能按比例、不匹配原因留痕与推荐定档）。JDK 21+ 仅可编译（`mvn compile`），无法运行规则引擎。

26 test cases in total: 5 rule-layer (DRL compiles, exclusive experience/performance ladders, tag provenance & dedup) + 5 service-layer (persistence filtering, field mapping, deactivate-then-save ordering) + 7 template-service (#14: real compilation validation, empty composite conditions rejected, skill/education array conditions) + 5 dynamic-rule (#16: persisted rules fire, recovery from DB after recreation, invalid rules rejected without persisting, delete/disable take effect) + 4 position-matching (#18: five-dimension scoring, 100-point cap, proportional partial-skill score, mismatch reasons and recommendation tiers). On JDK 21+ only compilation works; the rule engine cannot run.


---

## API 概览 / API Overview

| 分组 / Group                    | 端点 / Endpoint                          | 说明 / Description              |
|-------------------------------|------------------------------------------|--------------------------------|
| 规则管理 / Rule Management     | `/api/rule-management/**`                 | 规则 CRUD、启停、模板、验证、导入导出        |
| 标签库 / Tag Library           | `/api/tag-library-management/**`          | 标签 CRUD、分类、统计、导入导出             |
| 员工标签 / Employee Tagging    | `/api/enhanced-tagging/**`                | 单个/批量自动打标、标签统计、培训推荐       |
| 综合打标 / Integrated Tagging  | `/api/integrated-tagging/**`              | 动态规则打标、系统状态检查                 |
| 岗位匹配 / Position Matching   | `/api/matching/**`                        | 单人/批量匹配、推荐筛选、演示数据           |
| 员工与标签维护 / CRUD          | `/api/employees/**`、`/api/tags/**`、`/api/tag-categories/**` | 员工、标签、标签分类分页 CRUD |
---

## 已知限制 / Known Limitations

- **COMPOSITE 复合规则模板尚未完全实现**：`RuleTemplateService` 的复合模板生成目前为骨架实现，请优先使用其余模板或自定义 DRL。
  The COMPOSITE rule template is still a skeleton implementation; prefer the other templates or custom DRL.
- **运行时仅支持 JDK 8 ~ 17**：Drools 7.x 的 MVEL 依赖 `java.lang.Compiler`（JDK 21 起被移除），JDK 21+ 下可编译但规则引擎无法运行。
  Runtime requires JDK 8–17; on JDK 21+ the code compiles but the rule engine cannot run.
- **应用型参考实现**：本项目定位为可运行的应用示例，不提供可供其他 Maven 项目引用的库坐标。
  This project is a runnable application reference implementation; no library artifact is published for reuse as a Maven dependency.
- **无数据库启动为预览用途**：连接池使用懒初始化以支持免数据库预览（见「30 秒预览」）；生产部署建议移除该配置，使数据库不可用时启动即失败。
  Booting without a database is intended for preview only (lazy pool initialization); for production, remove that setting so a dead database fails the boot instead.

---

## 项目结构 / Directory Structure

```
drools-hr-tagging/
├── pom.xml                              # Maven 父 POM（多模块）
├── drools-hr-tagging-backend/           # 后端模块（Spring Boot 2.7 + Drools 7.73）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/northland/drools/
│       │   ├── DroolsApplication.java   # 启动类
│       │   ├── common/                  # 公共层：config / exception / response(ApiResponse)
│       │   ├── entity/                  # MyBatis-Plus 实体（Employee/Tag/TagCategory/TagRule/RuleTemplate/EmployeeTag/ProjectExperience）
│       │   ├── mapper/                  # MyBatis-Plus Mapper
│       │   ├── matching/                # 岗位匹配：controller / model(Employee,Position,MatchingResult) / service
│       │   │   └── config/DroolsConfig.java   # KieContainer 装配，启动时编译 3 份 DRL
│       │   └── tagging/                 # 标签系统：controller / model(EnhancedEmployee,Tag,TagRule...) / service
│       │       ├── service/impl/        # 员工、标签、分类、规则模板业务实现
│       │       └── model/dto/           # RuleExecuteResult / RuleValidateResult
│       ├── resources/
│       │   ├── application.properties   # 主配置（密码走环境变量 DB_PASSWORD）
│       │   └── rules/                   # DRL 规则
│       │       ├── employee-tagging-rules.drl          # 履历维度：经验 5 档互斥 + 学历/技能/领导力/潜力
│       │       ├── enhanced-employee-tagging-rules.drl # 考评维度：绩效 5 档互斥 + 成长/协作
│       │       └── position-matching-rules.drl         # 五维匹配评分规则
│       └── test/java/com/northland/drools/tagging/
│           ├── rules/EmployeeTaggingRulesTest.java              # 规则层：编译/互斥档位/标签完整性
│           └── service/impl/EmployeeTagServiceImplTest.java     # 服务层：落库过滤/映射/失效顺序
├── drools-hr-tagging-frontend/          # 前端模块（Vue 2.6 + Element UI）
│   ├── package.json
│   ├── vue.config.js                    # devServer 端口 9528
│   └── src/
│       ├── views/                       # Dashboard / TagLibrary / RuleManagement / EmployeeTagging / Statistics
│       ├── components/Layout/           # 侧边栏布局
│       ├── api/                         # Axios 接口封装
│       ├── router/ · store/ · utils/
│       └── assets/
├── database/create_tables.sql           # 建库建表 + 演示数据脚本
├── docs/                                # 教程与图片（建设中）
├── specs/                               # 规格说明（需求·设计·任务·验收）
├── AGENTS.md                            # AI 协作说明（架构约束、已知陷阱）
└── README.md · LICENSE · CHANGELOG.md · CONTRIBUTING.md
```

---

## 贡献指南 / Contributing

请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解如何参与贡献。

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to contribute.

---

## 许可证 / License

本项目基于 Apache License 2.0 许可证开源。详见 [LICENSE](LICENSE) 文件。

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
