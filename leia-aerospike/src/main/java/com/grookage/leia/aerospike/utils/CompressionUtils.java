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

package com.grookage.leia.aerospike.utils;

import com.grookage.leia.aerospike.storage.AerospikeRecord;
import com.grookage.leia.models.utils.MapperUtils;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@UtilityClass
public class CompressionUtils {

    @SneakyThrows
    public static String compress(final AerospikeRecord aerospikeRecord) {
        final var bos = new ByteArrayOutputStream();
        final var gzip = new GZIPOutputStream(bos);
        gzip.write(MapperUtils.mapper().writeValueAsBytes(aerospikeRecord));
        gzip.close();
        return bos.toString();
    }

    @SneakyThrows
    public static AerospikeRecord decompress(final String value) {
        final var gis = new GZIPInputStream(
                new ByteArrayInputStream(
                        value.getBytes(StandardCharsets.UTF_8)
                ));
        final var bf = new BufferedReader(new InputStreamReader(gis));
        final var stringBuilder = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null) {
            stringBuilder.append(line);
        }
        return MapperUtils.mapper().readValue(stringBuilder.toString(), AerospikeRecord.class);
    }
}
