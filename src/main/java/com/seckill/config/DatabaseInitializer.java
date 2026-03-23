package com.seckill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            fixUserTable();
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }

    private void fixUserTable() {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'"
            );

            boolean hasRoleType = false;
            boolean hasRole = false;
            
            log.info("检查 sys_user 表结构，发现以下列:");
            for (Map<String, Object> col : columns) {
                String colName = col.get("COLUMN_NAME").toString().toLowerCase();
                log.info("  - {}", colName);
                if ("role_type".equals(colName)) {
                    hasRoleType = true;
                }
                if ("role".equals(colName)) {
                    hasRole = true;
                }
            }

            if (hasRole && !hasRoleType) {
                log.info("发现列名不匹配，将 role 重命名为 role_type");
                jdbcTemplate.update(
                    "ALTER TABLE sys_user CHANGE COLUMN role role_type " +
                    "TINYINT NOT NULL DEFAULT 0 COMMENT '角色类型: 0-普通用户, 1-管理员'"
                );
                log.info("列名重命名完成");
            } else if (!hasRoleType) {
                log.info("添加缺失的 role_type 列");
                jdbcTemplate.update(
                    "ALTER TABLE sys_user ADD COLUMN role_type " +
                    "TINYINT NOT NULL DEFAULT 0 COMMENT '角色类型: 0-普通用户, 1-管理员' AFTER phone"
                );
                log.info("role_type 列添加完成");
            } else {
                log.info("sys_user 表结构正常");
            }
        } catch (Exception e) {
            log.warn("检查表结构时出错: {}", e.getMessage());
            try {
                log.info("尝试创建 sys_user 表（如果不存在）");
                jdbcTemplate.update(
                    "CREATE TABLE IF NOT EXISTS sys_user (" +
                    "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID'," +
                    "username VARCHAR(50) NOT NULL COMMENT '用户名'," +
                    "password VARCHAR(100) NOT NULL COMMENT '密码'," +
                    "nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称'," +
                    "email VARCHAR(100) DEFAULT NULL COMMENT '邮箱'," +
                    "phone VARCHAR(20) DEFAULT NULL COMMENT '手机号'," +
                    "role_type TINYINT NOT NULL DEFAULT 0 COMMENT '角色类型: 0-普通用户, 1-管理员'," +
                    "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除'," +
                    "PRIMARY KEY (id)," +
                    "UNIQUE INDEX uk_username (username)," +
                    "INDEX idx_role_type (role_type)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表'"
                );
                log.info("sys_user 表创建完成");
            } catch (Exception e2) {
                log.error("创建表失败: {}", e2.getMessage());
            }
        }
    }
}
