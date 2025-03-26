package com.grookage.leia.common.stubs;

import com.grookage.leia.models.annotations.attribute.qualifiers.Encrypted;
import com.grookage.leia.models.annotations.attribute.qualifiers.PII;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class TestParameterizedStub {
    String[] values;
    List<@PII PIIData> piiDataList;
    @Encrypted
    Map<TestEnum, String> map;
}
