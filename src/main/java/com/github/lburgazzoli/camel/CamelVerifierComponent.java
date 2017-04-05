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

import java.util.Map;

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
import org.apache.camel.util.ObjectHelper;

public class CamelVerifierComponent extends DefaultComponent {
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        return new DefaultEndpoint(uri, this) {
            @Override
            public boolean isSingleton() {
                return true;
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

                        final Component component = getCamelContext().getComponent(componentName, true, false);
                        if (component instanceof VerifiableComponent) {
                            ComponentVerifier verifier = ((VerifiableComponent) component).getVerifier();
                            in.setBody(verifier.verify(scope, componentOpts));
                        } else {
                            in.setBody(ResultBuilder.unsupported().build());
                        }
                    }
                };
            }

            @Override
            public Consumer createConsumer(Processor processor) throws Exception {
                throw new UnsupportedOperationException();
            }
        };
    }
}
