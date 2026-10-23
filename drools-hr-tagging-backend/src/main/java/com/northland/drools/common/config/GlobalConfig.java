package com.northland.drools.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 全局配置类
 */
@Configuration
public class GlobalConfig implements WebMvcConfigurer {

    /**
     * 允许的跨域来源（逗号分隔），走配置项 app.cors.allowed-origins，
     * 可用环境变量 CORS_ALLOWED_ORIGINS 覆盖（见 application.properties）。
     * 不再使用 "*" 通配 + 凭据的组合（#14）：通配来源与 allowCredentials 并用
     * 等于向任意站点开放带凭据的跨域请求。
     */
    @Value("${app.cors.allowed-origins:http://localhost:9528,http://localhost:8080}")
    private List<String> allowedOrigins;

    /**
     * 跨域配置（全局唯一来源，各 Controller 不再单独标注 @CrossOrigin）
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : allowedOrigins) {
            config.addAllowedOrigin(origin.trim());
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }

    /**
     * 静态资源配置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/");
    }
}
