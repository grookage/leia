package com.grookage.leia.aerospike.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AerospikeConfig {

    @NotEmpty
    private List<AerospikeHost> hosts;
    private String namespace;
    private String username;
    private String password;
    private boolean tlsEnabled = false;

    @Builder.Default
    private int maxConnectionsPerNode = 10;
    @Builder.Default
    private int scanMaxConcurrentNodes = 1;
    @Builder.Default
    private int batchMaxConcurrentNodes = 1;
    @Builder.Default
    private int timeout = 60;
    @Builder.Default
    private int retries = 3;
    @Builder.Default
    private int sleepBetweenRetries = 10;
    @Builder.Default
    private int threadPoolSize = 10;
    @Builder.Default
    private int maxSocketIdle = 10;
}
