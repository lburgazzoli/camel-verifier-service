package com.github.lburgazzoli.camel;

import groovy.lang.GroovyClassLoader;
import org.apache.camel.CamelContext;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.Registry;
import org.apache.camel.spring.spi.ApplicationContextRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CamelVerifierMain {
    @Autowired
    protected ApplicationContext applicationContext;
    @Autowired
    protected Predicate whiteList;

    @Bean
    public CamelContext camelContext() {
        Registry registry = new ApplicationContextRegistry(applicationContext);
        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("grape", new CamelGrapeComponent());
        context.addComponent("verifier", new CamelVerifierComponent());
        context.setApplicationContextClassLoader(new GroovyClassLoader(Thread.currentThread().getContextClassLoader()));

        return context;
    }

    @Bean
    public RouteBuilder routeBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                rest("/verify").description("CamelVerifier REST service")
                    .post()
                    .route()
                        .unmarshal()
                            .json(JsonLibrary.Jackson)
                        .setHeader("CamelVerifierComponent").simple("body[component]")
                        .setHeader("CamelVerifierOptions").simple("body[options]")
                        .setHeader("CamelVerifierScope").simple("body[scope]")
                        .setHeader("CamelGrapeMavenCoordinates").simple("body[gav]")
                        .filter(whiteList)
                            .to("grape:grab")
                            .to("verifier:verify")
                            .marshal()
                                .json(JsonLibrary.Jackson);
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(CamelVerifierMain.class, args);
    }
}
