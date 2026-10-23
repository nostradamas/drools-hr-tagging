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
- **版本管理**: 规则版本溯源与回滚

### 员工标签 / Employee Tagging
- **多维度自动打标**: 基于 Drools 规则引擎，从技能、经验、职级、绩效等多维度自动为员工打标
- **自定义标签规则**: 业务人员可通过可视化界面自定义打标逻辑
- **实时计算**: 员工信息变更时实时触发规则重算

### 岗位匹配 / Position Matching
- **技能匹配**: 基于技能标签库进行精准匹配
- **经验匹配**: 工作年限与领域经验匹配
- **学历匹配**: 学历与专业要求匹配
- **综合评分**: 多维度加权评分，智能推荐最优人选

### 标签库 / Tag Library
- **可管理标签系统**: 支持标签的增删改查、分类管理
- **标签层级**: 支持父子标签、标签分组
- **标签统计**: 查看每个标签的员工覆盖率

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
| JDK               | 8+            |
| Maven             | 3.6+          |
| MySQL             | 5.7+          |
| Node.js           | 14+           |

### 安装步骤 / Installation

```bash
# 1. 克隆项目 / Clone the repository
git clone https://github.com/your-org/drools-rule-engine-hr.git
cd drools-rule-engine-hr

# 2. 创建数据库 / Create the database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS drools_hr DEFAULT CHARSET utf8mb4;"

# 3. 修改数据库配置 / Modify database configuration
#    编辑 application.properties 中的数据库连接信息
#    Edit datasource settings in application.properties

# 4. 启动后端 / Start the backend
mvn spring-boot:run

# 5. 启动前端 / Start the frontend (新开终端 / new terminal)
cd frontend
npm install
npm run serve
```

访问 / Access: http://localhost:8080 (后端 / Backend) | http://localhost:9528 (前端 / Frontend)

---

## API 概览 / API Overview

| 分组 / Group               | 端点 / Endpoint                   | 说明 / Description                           |
|---------------------------|-----------------------------------|----------------------------------------------|
| 规则管理 / Rule Management | `/api/rules/**`                   | 规则的 CRUD、发布、回滚                        |
| 标签库 / Tag Library       | `/api/tags/**`                    | 标签分类、标签 CRUD、标签统计                 |
| 增强打标 / Enhanced Tagging | `/api/enhanced-tags/**`          | 批量打标、规则匹配打标                        |
| 综合打标 / Integrated Tagging | `/api/integrated-tags/**`      | 多维综合打标、复合规则打标                    |
| 匹配服务 / Matching        | `/api/matching/**`                | 岗位匹配、候选人推荐、评分明细                 |
| 系统状态 / System Status   | `/api/system/**`                  | 健康检查、规则引擎状态、缓存统计               |

---

## 项目结构 / Directory Structure

```
drools-rule-engine-hr/
├── README.md                          # 项目说明文档
├── LICENSE                            # Apache 2.0 许可证
├── CHANGELOG.md                       # 变更日志
├── CONTRIBUTING.md                    # 贡献指南
├── .editorconfig                      # 编辑器配置
├── .gitignore                         # Git 忽略规则
├── pom.xml                            # Maven 父 POM
├── sql/                               # 数据库初始化脚本
│   └── init.sql
├── src/
│   ├── main/
│   │   ├── java/com/drools/hr/
│   │   │   ├── HrApplication.java              # 启动类
│   │   │   ├── config/                          # 配置类
│   │   │   │   ├── DroolsConfig.java            # Drools 引擎配置
│   │   │   │   ├── MyBatisPlusConfig.java       # MyBatis-Plus 配置
│   │   │   │   └── CorsConfig.java              # 跨域配置
│   │   │   ├── controller/                      # REST 控制器
│   │   │   │   ├── RuleController.java
│   │   │   │   ├── TagController.java
│   │   │   │   ├── EmployeeTagController.java
│   │   │   │   ├── MatchingController.java
│   │   │   │   └── SystemController.java
│   │   │   ├── service/                         # 业务逻辑层
│   │   │   │   ├── RuleService.java
│   │   │   │   ├── TagService.java
│   │   │   │   ├── TaggingService.java
│   │   │   │   └── MatchingService.java
│   │   │   ├── entity/                          # 实体类
│   │   │   │   ├── Rule.java
│   │   │   │   ├── Tag.java
│   │   │   │   ├── Employee.java
│   │   │   │   └── EmployeeTag.java
│   │   │   ├── mapper/                          # MyBatis-Plus Mapper
│   │   │   ├── dto/                             # 数据传输对象
│   │   │   ├── vo/                              # 视图对象
│   │   │   └── common/                          # 公共工具
│   │   │       ├── Result.java                  # 统一响应
│   │   │       └── BaseEntity.java              # 实体基类
│   │   └── resources/
│   │       ├── application.properties           # 主配置
│   │       ├── application-dev.properties       # 开发环境配置
│   │       ├── rules/                           # Drools 规则文件 (.drl)
│   │       │   ├── tagging/                     # 标签规则
│   │       │   └── matching/                    # 匹配规则
│   │       └── mapper/                          # XML Mapper
│   └── test/
│       └── java/com/drools/hr/
│           ├── service/                         # 服务层测试
│           └── rule/                            # 规则单元测试
├── frontend/
│   ├── src/
│   │   ├── views/
│   │   │   ├── rule/                            # 规则管理页面
│   │   │   ├── tag/                             # 标签管理页面
│   │   │   ├── employee/                        # 员工管理页面
│   │   │   └── matching/                        # 岗位匹配页面
│   │   ├── router/
│   │   ├── store/
│   │   ├── api/                                 # API 接口封装
│   │   └── utils/
│   └── package.json
└── docs/                                        # 项目文档
```

---

## 贡献指南 / Contributing

请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解如何参与贡献。

Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to contribute.

---

## 许可证 / License

本项目基于 Apache License 2.0 许可证开源。详见 [LICENSE](LICENSE) 文件。

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
