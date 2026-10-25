# 贡献指南

感谢您对 **Drools 规则引擎管理系统** 的关注与支持！我们欢迎所有形式的贡献，包括但不限于：提交 Bug 报告、功能建议、代码贡献、文档改进等。

## 目录

- [行为准则](#行为准则)
- [报告问题](#报告问题)
- [功能建议](#功能建议)
- [提交代码](#提交代码)
  - [分支策略](#分支策略)
  - [开发流程](#开发流程)
  - [本地验证清单](#本地验证清单)
  - [代码规范](#代码规范)
  - [提交信息规范](#提交信息规范)
- [代码审查](#代码审查)
- [发布流程](#发布流程)

## 行为准则

请保持开放、包容、尊重他人的态度参与本项目。任何形式的骚扰、歧视或不当行为都是不被允许的。

## 报告问题

在提交 Issue 前，请先搜索是否已存在类似的问题。

请使用我们的 [Bug 报告模板](.github/ISSUE_TEMPLATE/bug_report.md) 提交问题，并尽可能提供以下信息：

- 问题的清晰描述
- 复现步骤
- 期望行为与实际行为
- 环境信息（操作系统、JDK 版本、浏览器版本等）
- 相关日志或截图

## 功能建议

欢迎通过 [功能建议模板](.github/ISSUE_TEMPLATE/feature_request.md) 提交新功能建议。请清晰描述：

- 需求背景与使用场景
- 期望的解决方案
- 是否有替代方案可以考虑

## 提交代码

### 分支策略

本项目采用以下分支管理策略：

- `main` — 唯一长期分支，保持稳定，随时可发布；所有 PR 均提向 `main`
- `feature/<简述>` — 功能分支，从 `main` 检出，如 `feature/rule-versioning`
- `fix/<Issue号>-<简述>` — 修复分支，从 `main` 检出，如 `fix/18-matching-drl`

### 开发流程

1. Fork 本仓库并克隆到本地
2. 从 `main` 创建分支：`git checkout -b feature/my-feature main`（修复用 `fix/<Issue号>-<简述>`）
3. 在分支上进行开发
4. 编写或更新测试用例，确保所有测试通过
5. 确保代码风格符合项目规范
6. 提交代码（请遵循提交信息规范）
7. 推送到你的 Fork 仓库
8. 向 `main` 分支发起 Pull Request

### 本地验证清单

提交 PR 前请在本地完成以下自查：

- [ ] JDK 为 8 ~ 17 任一版本（Drools 7.x 不兼容 JDK 21+，运行时会报错）
- [ ] 后端全量测试通过：`mvn -B clean test -f drools-hr-tagging-backend/pom.xml`
- [ ] 新增/修改的业务逻辑带有测试；PR 的 CI（JDK 8 + 17 双矩阵）必须全绿
- [ ] 涉及的文档已同步（README / CHANGELOG 的 [Unreleased] 等）
- [ ] 提交信息符合 Conventional Commits 规范，且不含密码、密钥等敏感信息

### 代码规范

#### Java 代码

- 遵循阿里巴巴 Java 开发手册规范
- 使用 4 个空格缩进，不使用 Tab
- 类名使用 PascalCase，方法名和变量名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 方法体尽量简短，一个方法不超过 80 行
- 添加必要的 JavaDoc 注释，尤其是公共 API 方法
- 新增/修改的业务逻辑需带测试，PR 的 CI（JDK 8 + 17）必须全绿

#### 前端代码 (Vue)

- 使用 2 个空格缩进
- 组件名使用 PascalCase
- Vue 文件遵循官方风格指南
- 组件内部属性顺序：name > components > props > data > computed > watch > 生命周期 > methods
- API 调用统一封装在 `src/api/` 目录下

#### SQL 代码

- 关键字使用大写（SELECT, FROM, WHERE 等）
- 表名和字段名使用小写蛇形命名
- 每张表必须包含 `id`, `create_time`, `update_time` 字段

### 提交信息规范

本项目采用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>
```

**type 类型：**

| 类型       | 说明                                         |
|-----------|----------------------------------------------|
| feat      | 新功能                                       |
| fix       | Bug 修复                                     |
| docs      | 文档变更                                     |
| style     | 代码格式（不影响功能的变更）                   |
| refactor  | 代码重构                                     |
| perf      | 性能优化                                     |
| test      | 添加或修改测试                               |
| chore     | 构建过程或辅助工具的变更                       |
| ci        | CI 配置变更                                  |

**scope 可选值：**

- `rule` — 规则管理模块
- `tag` — 标签管理模块
- `matching` — 岗位匹配模块
- `employee` — 员工管理模块
- `system` — 系统模块
- `frontend` — 前端修改
- `config` — 配置相关
- `docs` — 文档

**示例：**

```
feat(rule): 添加规则版本管理功能

- 支持规则版本创建与回滚
- 新增规则版本历史查询接口
- 前端增加版本对比视图
```

## 代码审查

所有 Pull Request 必须经过至少一名维护者的 Code Review 后方可合并。

审查要点：

- 代码正确性：逻辑是否正确，边界情况是否处理
- 代码风格：是否符合项目规范
- 测试覆盖：是否有充分的单元测试
- 性能影响：是否引入性能问题
- 安全性：是否存在安全漏洞

## 发布流程

1. 确认 `main` 上 CI 全绿，CHANGELOG 的 [Unreleased] 已整理为新版本条目
2. 在 `main` 上打版本 Tag（如 `v1.1.0`）
3. 基于 Tag 创建 GitHub Release，附更新说明

---

再次感谢您的贡献！
