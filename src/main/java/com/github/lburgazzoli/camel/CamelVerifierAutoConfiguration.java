/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.lburgazzoli.camel;

import groovy.lang.GroovyClassLoader;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.Registry;
import org.apache.camel.spring.spi.ApplicationContextRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "camel.verifier.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CamelVerifierConfigurationProperties.class)
public class CamelVerifierAutoConfiguration {
    @Autowired
    CamelVerifierConfigurationProperties configuration;
    @Autowired
    protected ApplicationContext applicationContext;

    @Bean
    public CamelVerifierComponentFilter componentFilter() {
        return exchange ->  {
            String component = exchange.getIn().getHeader("CamelVerifierComponent", String.class);
            return configuration.getComponents().contains(component);
        };
    }

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
    public RouteBuilder routeBuilder(CamelVerifierComponentFilter componentFilter) {
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
                        .filter(componentFilter)
                            .to("grape:grab")
                            .to("verifier:verify")
                                .marshal()
                                .json(JsonLibrary.Jackson);
            }
        };
    }
}
