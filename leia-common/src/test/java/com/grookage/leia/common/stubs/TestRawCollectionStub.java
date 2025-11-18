package com.grookage.leia.common.stubs;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.*;

@Data
@Builder
@Jacksonized
public class TestRawCollectionStub {
	List rawList;
	LinkedList rawLinkedList;
	Set rawSet;
	HashSet rawHashSet;
	Map rawMap;
	SortedMap rawSortedMap;
}
