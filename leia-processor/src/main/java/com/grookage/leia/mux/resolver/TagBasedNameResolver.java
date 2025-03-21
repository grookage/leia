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

package com.grookage.leia.mux.resolver;

import com.grookage.leia.models.mux.LeiaMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

@AllArgsConstructor
@Data
public class TagBasedNameResolver implements BackendNameResolver {

    private static final String BACKEND_TAG = "backend";
    private static final String TAG_SEPARATOR = "-";

    private final Supplier<List<String>> backends;

    @Override
    public List<String> getEligibleBackends(LeiaMessage leiaMessage) {
        final var tags = leiaMessage.getTags();
        if (null == tags || tags.isEmpty()) {
            return List.of();
        }
        final var backendTag = tags.stream()
                .filter(each -> each.contains(BACKEND_TAG)).findFirst().orElse(null);
        if (null == backendTag) {
            return List.of();
        }
        final List<String> eligibleBackends = null == backends ? List.of() :
                Objects.requireNonNullElse(backends.get(), List.<String>of()).stream().map(String::toUpperCase).toList();
        final var configuredBackends = Arrays.asList(backendTag.toUpperCase(Locale.ROOT).substring(backendTag.lastIndexOf(TAG_SEPARATOR) + 1).split("\\s*::\\s*"));
        return eligibleBackends.stream()
                .filter(configuredBackends::contains)
                .toList();
    }
}
