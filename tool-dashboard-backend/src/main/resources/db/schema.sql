-- 创建数据库
CREATE DATABASE IF NOT EXISTS `tool_dashboard` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `tool_dashboard`;

-- 工具表
CREATE TABLE `tb_tool` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '工具名称',
  `description` varchar(500) DEFAULT NULL COMMENT '工具描述',
  `type` varchar(50) DEFAULT NULL COMMENT '工具类型',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '工具图标URL',
  `access_url` varchar(255) DEFAULT NULL COMMENT '工具访问地址',
  `current_version` varchar(50) DEFAULT NULL COMMENT '当前版本',
  `owner` varchar(50) DEFAULT NULL COMMENT '负责人',
  `status` int(11) DEFAULT '1' COMMENT '状态（0-已下线 1-运行中 2-维护中）',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '逻辑删除（0-未删除 1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具表';

-- 工具文件表
CREATE TABLE `tb_tool_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tool_id` bigint(20) NOT NULL COMMENT '关联工具ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `original_name` varchar(255) DEFAULT NULL COMMENT '文件原始名称',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(100) DEFAULT NULL COMMENT '文件类型',
  `version` varchar(50) DEFAULT NULL COMMENT '版本号',
  `download_count` int(11) DEFAULT '0' COMMENT '下载次数',
  `description` varchar(500) DEFAULT NULL COMMENT '文件说明',
  `uploader` varchar(50) DEFAULT NULL COMMENT '上传者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '逻辑删除（0-未删除 1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tool_id` (`tool_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具文件表';

-- 变更日志表
CREATE TABLE `tb_change_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tool_id` bigint(20) NOT NULL COMMENT '关联工具ID',
  `version` varchar(50) DEFAULT NULL COMMENT '版本号',
  `change_type` varchar(50) DEFAULT NULL COMMENT '变更类型（新增/修复/优化/删除）',
  `content` text COMMENT '变更内容',
  `changer` varchar(50) DEFAULT NULL COMMENT '变更人',
  `change_time` datetime DEFAULT NULL COMMENT '变更时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '逻辑删除（0-未删除 1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tool_id` (`tool_id`),
  KEY `idx_change_time` (`change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='变更日志表';

-- 管理员表
CREATE TABLE `tb_admin` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（加密存储）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` int(11) DEFAULT '1' COMMENT '状态（0-禁用 1-启用）',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(11) DEFAULT '0' COMMENT '逻辑删除（0-未删除 1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 插入默认管理员账号（用户名：admin，密码：admin123）
INSERT INTO `tb_admin` (`username`, `password`, `real_name`, `email`, `status`) 
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '系统管理员', 'admin@example.com', 1);
