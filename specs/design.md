# 设计规格说明书

## 1. 架构设计

### 1.1 系统架构
```
┌─────────────────────────────────────────────────────┐
│                    Frontend (Vue 2)                  │
│  Element UI · Vue Router · Vuex · Axios             │
└────────────────────────┬────────────────────────────┘
                         │ REST API
┌────────────────────────▼────────────────────────────┐
│                 Backend (Spring Boot)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────────┐ │
│  │ Controller│ │ Service  │ │ Drools Rule Engine   │ │
│  │   Layer   │ │  Layer   │ │  (KIE Session)       │ │
│  └──────────┘ └──────────┘ └──────────────────────┘ │
│                        │                             │
│  ┌─────────────────────▼───────────────────────────┐│
│  │          MyBatis-Plus + MySQL                    ││
│  └─────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────┘
```

### 1.2 核心数据模型

#### employee（员工信息表）
基础字段 + 能力素质(JSON) + 履历信息(JSON) + 考评信息(JSON)

#### tag（标签库表）
编码·名称·描述·分类·维度·优先级·权重·颜色

#### employee_tag（员工标签关联）
员工ID·标签编码·置信度·来源·原因·过期时间

#### tag_rule（标签规则表）
规则编码·标签编码·规则内容(DRL)·模板·条件(JSON)·优先级·版本

#### project_experience（项目经验表）
员工ID·项目名称·角色·时长·技术栈·成果

## 2. API 接口定义

### 2.1 规则管理 (/api/rule-management)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /rules | 获取所有规则 |
| GET | /rules/{code} | 获取规则详情 |
| GET | /rules/tag/{tagCode} | 按标签获取规则 |
| POST | /rules | 创建规则 |
| PUT | /rules/{code} | 更新规则 |
| DELETE | /rules/{code} | 删除规则 |
| POST | /rules/{code}/toggle | 启用/禁用 |
| POST | /rules/{code}/test | 测试规则 |
| POST | /rules/batch | 批量操作 |
| POST | /rules/initialize | 初始化默认规则 |
| POST | /rules/preview | 预览规则内容 |
| POST | /rules/validate | 验证规则语法 |
| GET | /templates | 获取规则模板 |
| GET | /statistics | 获取规则统计 |
| GET | /export | 导出规则 |
| POST | /import | 导入规则 |

### 2.2 标签库管理 (/api/tag-library-management)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /tags | 获取所有标签 |
| GET | /tags/{code} | 获取标签详情 |
| GET | /tags/dimension/{dim} | 按维度获取标签 |
| GET | /tags/category/{cat} | 按分类获取标签 |
| GET | /tags/search | 搜索标签 |
| POST | /tags | 创建标签 |
| PUT | /tags/{code} | 更新标签 |
| DELETE | /tags/{code} | 删除标签 |
| POST | /tags/batch | 批量操作 |
| GET | /statistics | 获取标签统计 |
| GET | /categories | 获取标签分类 |
| GET | /export | 导出标签库 |
| POST | /import | 导入标签库 |

### 2.3 员工标签 (/api/enhanced-tagging)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /tag-employee | 单员工打标签 |
| POST | /batch-tag | 批量打标签 |
| GET | /employee/{id}/tag-stats | 获取标签统计 |
| GET | /employee/{id}/tags-by-dimension | 按维度获取标签 |
| POST | /employee/{id}/add-tag | 手动添加标签 |
| GET | /tag-library | 获取标签库 |
| GET | /tag-library/search | 搜索标签 |
| GET | /employee/{id}/training-recommendations | 培训推荐 |
| GET | /demo | 完整演示 |

### 2.4 岗位匹配 (/api/matching)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /single | 单员工单职位匹配 |
| POST | /employee-to-positions | 单员工多职位 |
| POST | /employees-to-position | 多员工单职位 |
| POST | /batch | 批量匹配 |
| POST | /recommended | 获取推荐 |
| GET | /test | 测试匹配 |
| GET | /test-data | 获取测试数据 |
| GET | /demo | 完整演示 |

## 3. 架构决策
- ADR-001: 规则引擎采用 Drools 7 KIE 原生 API，不引入 kie-spring 的 XML 配置
- ADR-002: 动态规则通过 KieFileSystem 编译，运行时刷新 KieContainer
- ADR-003: 标签过期策略按维度差异化配置（6月/1年/2年）
- ADR-004: 匹配分数采用累加制（基础 100 分制），不设扣分机制
