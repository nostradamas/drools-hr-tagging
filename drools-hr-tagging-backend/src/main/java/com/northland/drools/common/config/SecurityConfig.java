package com.northland.drools.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

/**
 * API 安全配置（#14）。
 *
 * <p>背景：规则管理写接口可提交并编译任意 DRL，而 DRL 的 then 段本质是 Java 代码，
 * 无认证时任何能访问端口的人都等效拥有服务端代码执行能力。因此：
 * <ul>
 *   <li>/api/rule-management/** 的写操作（POST/PUT/DELETE）要求 ADMIN 角色 + HTTP Basic；</li>
 *   <li>其余接口（含规则查询与其余演示/只读 API）保持 permitAll，不影响演示体验。</li>
 * </ul>
 *
 * <p>管理员账号走环境变量：用户名 {@code ADMIN_USERNAME}（默认 admin），
 * 密码 {@code ADMIN_PASSWORD}；未设置密码时启动时生成随机 UUID 密码并在日志中
 * 以 WARN 级别打印一次（仅本次启动有效），同时给出明确告警——生产部署务必显式设置。
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        String effectivePassword = adminPassword;
        if (effectivePassword == null || effectivePassword.trim().isEmpty()) {
            effectivePassword = UUID.randomUUID().toString();
            log.warn("============================================================");
            log.warn("未设置环境变量 ADMIN_PASSWORD，已为管理员账号 [{}] 生成随机密码（仅本次启动有效）：{}",
                    adminUsername, effectivePassword);
            log.warn("生产环境请务必通过 ADMIN_PASSWORD 显式设置管理员密码。");
            log.warn("============================================================");
        }
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(effectivePassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 本服务是无状态 REST API，不使用基于 Cookie/Session 的表单提交，
                // 认证走 HTTP Basic 请求头，故禁用 CSRF（与 Spring Security 对纯 API 的常规做法一致）。
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/api/rule-management/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/rule-management/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/rule-management/**").hasRole("ADMIN")
                .anyRequest().permitAll()
                .and()
                .httpBasic();
        return http.build();
    }
}
