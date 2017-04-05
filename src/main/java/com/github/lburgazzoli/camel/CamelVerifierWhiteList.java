package com.github.lburgazzoli.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.util.ObjectHelper;
import org.springframework.stereotype.Component;

@Component
public class CamelVerifierWhiteList implements Predicate {
    @Override
    public boolean matches(Exchange exchange) {
        String component = exchange.getIn().getHeader("CamelVerifierComponent", String.class);
        return ObjectHelper.equal("twitter", component);
    }
}
