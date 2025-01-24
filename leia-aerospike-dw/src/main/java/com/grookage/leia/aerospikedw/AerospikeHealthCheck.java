/*
 * Copyright (c) 2024. Koushik R <rkoushik.14@gmail.com>.
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

package com.grookage.leia.aerospikedw;

import com.google.common.base.Preconditions;
import com.grookage.leia.aerospike.client.AerospikeManager;
import com.grookage.leia.dropwizard.bundle.health.LeiaHealthCheck;
import lombok.SneakyThrows;

public class AerospikeHealthCheck extends LeiaHealthCheck {
    private final AerospikeManager aerospikeManager;

    public AerospikeHealthCheck(AerospikeManager aerospikeManager) {
        super("aerospike-health-check");
        Preconditions.checkNotNull(aerospikeManager, "Aerospike Manager can't be null");
        this.aerospikeManager = aerospikeManager;
    }

    @Override
    @SneakyThrows
    protected Result check() {
        final var healthStatus = aerospikeManager.getClient().isConnected();
        if (!healthStatus) {
            return Result.unhealthy("Last status: %s", "Not Connected");
        } else {
            return Result.healthy("Last status: %s", "Connected");
        }
    }
}
