/*
 * Copyright (c) 2025. Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grookage.leia.mux.filter;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

@Slf4j
class BackendFilterTest {

    @Test
    void testNoOpBackendFilter() {
        BackendFilter filter = new NoOpBackendFilter();

        Assertions.assertTrue(filter.shouldProcess("TEST"));
        Assertions.assertTrue(filter.shouldProcess("PROD"));
    }

    @Test
    void testWhitelistBackendFilter() {
        // Filter that only allows specific backends
        BackendFilter whitelistFilter = new WhitelistBackendFilter(Set.of("BACKEND1", "BACKEND2"));

        Assertions.assertTrue(whitelistFilter.shouldProcess("BACKEND1"));
        Assertions.assertTrue(whitelistFilter.shouldProcess("BACKEND2"));
        Assertions.assertFalse(whitelistFilter.shouldProcess("BACKEND3"));
    }

    @Test
    void testBlacklistBackendFilter() {
        // Filter that excludes specific backends
        BackendFilter blacklistFilter = new BlacklistBackendFilter(Set.of("PROD", "STAGING"));

        Assertions.assertTrue(blacklistFilter.shouldProcess("DEV"));
        Assertions.assertFalse(blacklistFilter.shouldProcess("PROD"));
        Assertions.assertFalse(blacklistFilter.shouldProcess("STAGING"));
    }

    @Test
    void testRegexBackendFilter() {
        // Filter based on regex pattern
        BackendFilter regexFilter = new RegexBackendFilter("^TEST.*");

        Assertions.assertTrue(regexFilter.shouldProcess("TEST-BACKEND1"));
        Assertions.assertTrue(regexFilter.shouldProcess("TEST-BACKEND2"));
        Assertions.assertFalse(regexFilter.shouldProcess("PROD-BACKEND"));
    }

    /**
     * Sample filter that only allows whitelisted backends
     */
    static class WhitelistBackendFilter implements BackendFilter {
        private final Set<String> allowedBackends;

        public WhitelistBackendFilter(Set<String> allowedBackends) {
            this.allowedBackends = allowedBackends;
        }

        @Override
        public boolean shouldProcess(String backendName) {
            boolean allowed = allowedBackends.contains(backendName);
            if (!allowed) {
                log.debug("Backend {} not in whitelist, filtering out", backendName);
            }
            return allowed;
        }
    }

    /**
     * Sample filter that excludes blacklisted backends
     */
    static class BlacklistBackendFilter implements BackendFilter {
        private final Set<String> excludedBackends;

        public BlacklistBackendFilter(Set<String> excludedBackends) {
            this.excludedBackends = excludedBackends;
        }

        @Override
        public boolean shouldProcess(String backendName) {
            boolean excluded = excludedBackends.contains(backendName);
            if (excluded) {
                log.debug("Backend {} is blacklisted, filtering out", backendName);
            }
            return !excluded;
        }
    }

    /**
     * Sample filter based on regex pattern
     */
    static class RegexBackendFilter implements BackendFilter {
        private final String pattern;

        public RegexBackendFilter(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean shouldProcess(String backendName) {
            boolean matches = backendName.matches(pattern);
            if (!matches) {
                log.debug("Backend {} does not match pattern {}, filtering out", backendName, pattern);
            }
            return matches;
        }
    }
}
