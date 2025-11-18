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

package com.grookage.leia.models.utils;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;

@UtilityClass
public class LeiaUtils {

	/*
	   If we know the upper bound condition, please use the until with the upper bound.
	   Only for cases, where you have to wait till the refreshInterval periods, don't want to introduce
	   refreshed and other boolean flags throughout the code.
	*/
	public static void sleepUntil(int numSeconds) {
		await().pollDelay(Duration.ofSeconds(numSeconds)).until(() -> true);
	}

	public static void sleepFor(int numSeconds) {
		await().atMost(Duration.ofSeconds(numSeconds)).until(() -> true);
	}

	/*
		Use this when you have to alter the numSeconds in any of the specific assertions. For finder and hub, the values are appropriately coded
		keeping the start intervals in mind.
	 */
	public static void sleepUntil(int numSeconds, Callable<Boolean> conditionEvaluator) {
		await().pollDelay(Duration.ofSeconds(numSeconds)).until(conditionEvaluator);
	}

}
