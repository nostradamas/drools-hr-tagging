# Drools HR Tagging — Drools 规则引擎实战示例

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.0-brightgreen" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Drools-7.73.0-blue" alt="Drools"/>
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.3-orange" alt="MyBatis-Plus"/>
  <img src="https://img.shields.io/badge/MySQL-5.7-yellow" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Vue-2.6-4FC08D" alt="Vue"/>
  <img src="https://img.shields.io/badge/Element%20UI-2.15-409EFF" alt="Element UI"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/Java-1.8-red" alt="Java"/>
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
│  │              Spring Boot 2.7.0 (REST Controller)          │  │
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
| Node.js           | 14+           |

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
cd drools-hr-tagging-frontend
npm install
npm run dev
```

访问 / Access: http://localhost:9528 (前端 / Frontend) | http://localhost:8080/api (后端 / Backend)

> 数据库默认连接 `localhost:3306/employee_tagging`，账号 root，密码取环境变量 `DB_PASSWORD`（默认 root）。

### 运行测试 / Running Tests

```bash
# 测试必须在 JDK 8 ~ 17 下运行（Drools 7.73 的 MVEL 依赖 JDK 21 起被移除的 java.lang.Compiler）
# Tests require JDK 8 ~ 17 (MVEL relies on java.lang.Compiler, removed in JDK 21)
export JAVA_HOME=$(/usr/libexec/java_home -v 11)   # macOS 示例 / macOS example
mvn clean test -f drools-hr-tagging-backend/pom.xml
```

当前 10 个测试用例：规则层 5 例（DRL 可编译、经验/绩效档位互斥、标签来源/原因/过期时间、无重复打标）+ 服务层 5 例（落库过滤条件、字段映射、旧标签失效顺序）。JDK 21+ 仅可编译（`mvn compile`），无法运行规则引擎。

10 test cases in total: 5 rule-layer (DRL compiles, exclusive experience/performance ladders, tag provenance & dedup) + 5 service-layer (persistence filtering, field mapping, deactivate-then-save ordering). On JDK 21+ only compilation works; the rule engine cannot run.


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
├── docs/ · specs/                       # 开发文档 / 规格说明（需求·设计·任务·验收）
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
