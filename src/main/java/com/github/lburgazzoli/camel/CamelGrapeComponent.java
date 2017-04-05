package com.github.lburgazzoli.camel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.grape.Grape;
import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.component.grape.GrapeComponent;
import org.apache.camel.component.grape.GrapeEndpoint;
import org.apache.camel.component.grape.MavenCoordinates;
import org.apache.camel.component.grape.PatchesRepository;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ExchangeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelGrapeComponent extends GrapeComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelGrapeComponent.class);

    public CamelGrapeComponent() {
        super();

        super.setPatchesRepository(new PatchesRepository() {
            private final List<String> elements = new ArrayList<>();

            @Override
            public void install(String s) {
                if (!elements.contains(s)) {
                    LOGGER.info("install {}", s);
                    elements.add(s);
                }
            }

            @Override
            public List<String> listPatches() {
                return elements;
            }

            @Override
            public void clear() {
                elements.clear();
            }
        });
    }


    @Override
    protected GrapeEndpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        return new GrapeEndpoint(uri, null, this) {
            @Override
            public Producer createProducer() {
                return new DefaultProducer(this) {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String rawCoordinates = ExchangeHelper.getMandatoryHeader(exchange, "CamelGrapeMavenCoordinates", String.class);
                        MavenCoordinates coordinates = MavenCoordinates.parseMavenCoordinates(rawCoordinates);

                        Map<String, Object> params = new HashMap<>();
                        params.put("classLoader", exchange.getContext().getApplicationContextClassLoader());
                        params.put("group", coordinates.getGroupId());
                        params.put("module", coordinates.getArtifactId());
                        params.put("version", coordinates.getVersion());
                        params.put("classifier", coordinates.getClassifier());

                        Grape.grab(params);

                        CamelGrapeComponent.this.getPatchesRepository().install(rawCoordinates);
                    }
                };
            }
        };
    }
}
