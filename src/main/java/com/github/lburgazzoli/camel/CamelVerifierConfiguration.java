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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camel.verifier")
public class CamelVerifierConfiguration {
    /**
     * White list components
     */
    private List<String> components = new ArrayList<>();

    /**
     * Grape configuration
     */
    private Grape grape = new Grape();

    public List<String> getComponents() {
        return components;
    }

    public Grape getGrape() {
        return grape;
    }

    public class Grape {
        /**
         * Additional maven repositories.
         */
        private Map<String, String> repositories = new HashMap<>();

        /**
         * Grape cache dir.
         */
        private String cacheDir;


        public Map<String, String> getRepositories() {
            return repositories;
        }

        public String getCacheDir() {
            return cacheDir;
        }
    }
}
