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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import groovy.grape.Grape;
import org.apache.camel.Component;
import org.apache.camel.ComponentVerifier;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.VerifiableComponent;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.impl.verifier.ResultBuilder;
import org.apache.camel.impl.verifier.ResultErrorBuilder;
import org.apache.camel.util.ObjectHelper;

public class CamelVerifierComponent extends DefaultComponent {
    private final CamelVerifierConfiguration configuration;

    public CamelVerifierComponent(CamelVerifierConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (ObjectHelper.equal("verify", remaining, true)) {
            return new VerifierProducer(uri, this);
        }
        if (ObjectHelper.equal("grab", remaining, true)) {
            return new GrabProducer(uri, this);
        }

        throw new IllegalArgumentException(remaining);
    }

    // **********************************************
    // Verifier Producer
    // **********************************************

    private final class VerifierProducer extends DefaultEndpoint {

        public VerifierProducer(String endpointUri, Component component) {
            super(endpointUri, component);
        }

        @Override
        public Producer createProducer() throws Exception {
            return new DefaultProducer(this) {
                @Override
                public void process(Exchange exchange) throws Exception {
                    final Message in = exchange.getIn();
                    final String componentName = in.getHeader("CamelVerifierComponent", String.class);
                    final Map<String, Object> componentOpts = in.getHeader("CamelVerifierOptions", Map.class);
                    final ComponentVerifier.Scope scope = in.getHeader("CamelVerifierScope", ComponentVerifier.Scope.class);

                    ObjectHelper.notNull(componentName, "CamelVerifierComponent");
                    ObjectHelper.notNull(componentOpts, "CamelVerifierOptions");
                    ObjectHelper.notNull(scope, "CamelVerifierScope");

                    boolean supported = configuration.getComponents().contains(componentName);
                    if (supported) {
                        final Component component = getCamelContext().getComponent(componentName, true, false);
                        if (component instanceof VerifiableComponent) {
                            ComponentVerifier verifier = ((VerifiableComponent) component).getVerifier();
                            in.setBody(verifier.verify(scope, componentOpts));
                        } else {
                            supported = false;
                        }
                    }

                    if (!supported) {
                        in.setBody(
                            ResultBuilder.withScope(scope)
                                .error(ResultErrorBuilder.withUnsupportedComponent(componentName).build())
                                .build()
                        );
                    }
                }
            };
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }


    // **********************************************
    // Grab Producer
    // **********************************************

    private final class GrabProducer extends DefaultEndpoint {

        public GrabProducer(String endpointUri, Component component) {
            super(endpointUri, component);

            if (ObjectHelper.isNotEmpty(configuration.getGrape().getCacheDir())) {
                System.setProperty("grape.root", configuration.getGrape().getCacheDir());
            }

            Grape.setEnableGrapes(true);
            Grape.setEnableAutoDownload(true);

            configuration.getGrape().getRepositories().entrySet().forEach(
                e -> Grape.addResolver(Collections.singletonMap(e.getKey(), e.getValue()))
            );
        }

        @Override
        public Producer createProducer() throws Exception {
            return new DefaultProducer(this) {
                @Override
                public void process(Exchange exchange) throws Exception {
                    String coordinates = exchange.getIn().getHeader("CamelGrapeMavenCoordinates", String.class);

                    if (ObjectHelper.isNotEmpty(coordinates)) {
                        Map<String, Object> params = parseMavenCoordinates(coordinates);
                        params.put("classLoader", exchange.getContext().getApplicationContextClassLoader());

                        Grape.grab(params);
                    }
                }
            };
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSingleton() {
            return true;
        }

        private Map<String, Object> parseMavenCoordinates(String coordinates) {
            String[] parts = coordinates.split("/");
            if (parts.length < 3 || parts.length > 4) {
                throw new IllegalArgumentException("Invalid coordinates: " + coordinates);
            }

            Map<String, Object> answer = new HashMap<>();
            answer.put("group", parts[0]);
            answer.put("module", parts[1]);
            answer.put("version", parts[2]);
            answer.put("classifier", parts.length == 4 ? parts[3] : "");

            return answer;
        }
    }
}
