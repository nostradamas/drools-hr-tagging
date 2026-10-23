package com.northland.drools.matching.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drools 规则引擎配置。
 * 仅暴露 KieContainer 单例；KieSession 由各服务按请求创建并在用完后 dispose，
 * 避免共享会话的状态污染与并发问题。
 */
@Configuration
public class DroolsConfig {

    private static final String RULES_PATH = "rules/";

    @Bean
    public KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH + "employee-tagging-rules.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH + "enhanced-employee-tagging-rules.drl"));
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH + "position-matching-rules.drl"));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Drools 规则编译失败:\n" + kieBuilder.getResults().getMessages(Message.Level.ERROR));
        }

        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
