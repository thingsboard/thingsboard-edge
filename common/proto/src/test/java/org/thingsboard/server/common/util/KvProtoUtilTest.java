/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.kv.AggTsKvEntry;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class KvProtoUtilTest {

    private static final long TS = System.currentTimeMillis();

    private static Stream<KvEntry> kvEntryData() {
        String key = "key";
        return Stream.of(
                new BooleanDataEntry(key, true),
                new LongDataEntry(key, 23L),
                new DoubleDataEntry(key, 23.0),
                new StringDataEntry(key, "stringValue"),
                new JsonDataEntry(key, "jsonValue")
        );
    }

    private static Stream<KvEntry> basicTsKvEntryData() {
        return kvEntryData().map(kvEntry -> new BasicTsKvEntry(TS, kvEntry));
    }

    private static Stream<List<BaseAttributeKvEntry>> attributeKvEntryData() {
        return Stream.of(kvEntryData().map(kvEntry -> new BaseAttributeKvEntry(TS, kvEntry)).toList());
    }

    private static List<TsKvEntry> createTsKvEntryList(boolean withAggregation) {
        return kvEntryData().map(kvEntry -> {
                    if (withAggregation) {
                        return new AggTsKvEntry(TS, kvEntry, 0);
                    } else {
                        return new BasicTsKvEntry(TS, kvEntry);
                    }
                }).collect(Collectors.toList());
    }

    @ParameterizedTest
    @EnumSource(DataType.class)
    void protoDataTypeSerialization(DataType dataType) {
        assertThat(KvProtoUtil.fromKeyValueTypeProto(KvProtoUtil.toKeyValueTypeProto(dataType)))
                .as(dataType.name()).isEqualTo(dataType);
    }

    @ParameterizedTest
    @MethodSource("kvEntryData")
    void protoKeyValueProtoSerialization(KvEntry kvEntry) {
        assertThat(KvProtoUtil.fromTsKvProto(KvProtoUtil.toKeyValueTypeProto(kvEntry)))
                .as("deserialized").isEqualTo(kvEntry);
    }

    @ParameterizedTest
    @MethodSource("basicTsKvEntryData")
    void protoTsKvEntrySerialization(KvEntry kvEntry) {
        assertThat(KvProtoUtil.fromTsKvProto(KvProtoUtil.toTsKvProto(TS, kvEntry)))
                .as("deserialized").isEqualTo(kvEntry);
    }

    @ParameterizedTest
    @MethodSource("kvEntryData")
    void protoTsValueSerialization(KvEntry kvEntry) {
        assertThat(KvProtoUtil.fromTsValueProto(kvEntry.getKey(), KvProtoUtil.toTsValueProto(TS, kvEntry)))
                .as("deserialized").isEqualTo(kvEntry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void protoListTsKvEntrySerialization(boolean withAggregation) {
        List<TsKvEntry> tsKvEntries = createTsKvEntryList(withAggregation);
        assertThat(KvProtoUtil.fromTsKvProtoList(KvProtoUtil.toTsKvProtoList(tsKvEntries)))
                .as("deserialized").isEqualTo(tsKvEntries);
    }

    @ParameterizedTest
    @MethodSource("attributeKvEntryData")
    void protoListAttributeKvSerialization(List<AttributeKvEntry> attributeKvEntries) {
        assertThat(KvProtoUtil.toAttributeKvList(KvProtoUtil.attrToTsKvProtos(attributeKvEntries)))
                .as("deserialized")
                .isEqualTo(attributeKvEntries);
    }

}
