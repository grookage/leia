package com.grookage.leia.common.stubs;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@Jacksonized
public class TestObjectStub {
	Object object;
	Object[] objects;
	List<Object> objectList;
	Set<Object> objectSet;
	Map<String, Object> objectMap;
}
