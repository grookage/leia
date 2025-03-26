package com.grookage.leia.common.stubs;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class TestGenericStub {
    GenericStub<Integer, String> genericStub;
}
