-- =============================================
-- 增量补表脚本（Issue #23）
-- 适用：已用旧版 create_tables.sql 初始化过的存量库
-- （含 docker compose 已有 drools-mysql-data 数据卷的部署）
-- 幂等：全部 CREATE TABLE IF NOT EXISTS，可重复执行；
-- 不 DROP、不修改、不删除任何现有数据。
-- 用法：mysql -u root -p employee_tagging < database/add_tag_category_rule_template.sql
-- =============================================
USE `employee_tagging`;

CREATE TABLE IF NOT EXISTS `tag_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `description` text COMMENT '分类描述',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签分类表';

CREATE TABLE IF NOT EXISTS `rule_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `description` text COMMENT '模板描述',
  `rule_content` longtext COMMENT '规则内容(DRL格式)',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_name` (`template_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则模板表';

-- 执行后自检：应返回 2 行
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'employee_tagging'
  AND TABLE_NAME IN ('tag_category', 'rule_template');
