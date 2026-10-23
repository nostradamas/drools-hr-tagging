package com.northland.drools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Drools Rules Engine - 统一应用启动类
 * <p>
 * 整合员工标签管理、人才匹配和规则引擎功能。
 * 提供基于 Drools 的智能规则引擎，支持动态规则管理与员工标签画像。
 *
 * @author nostradamas
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class DroolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroolsApplication.class, args);

        System.out.println("============================================");
        System.out.println("  Drools Rules Engine 启动成功!");
        System.out.println("  访问地址: http://localhost:8080");
        System.out.println("  前端页面: http://localhost:8080/index.html");
        System.out.println("============================================");
        System.out.println("  功能模块:");
        System.out.println("  1. 员工标签管理 - /api/enhanced-tagging");
        System.out.println("  2. 规则管理     - /api/rule-management");
        System.out.println("  3. 标签库管理   - /api/tag-library-management");
        System.out.println("  4. 集成标签服务 - /api/integrated-tagging");
        System.out.println("  5. 员工匹配     - /api/matching");
        System.out.println("============================================");
    }
}
