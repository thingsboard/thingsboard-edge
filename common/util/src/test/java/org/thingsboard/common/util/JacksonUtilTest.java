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
package org.thingsboard.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonUtilTest {

    @Test
    public void allowUnquotedFieldMapperTest() {
        String data = "{data: 123}";
        JsonNode actualResult = JacksonUtil.toJsonNode(data, JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER); // should be: {"data": 123}
        ObjectNode expectedResult = JacksonUtil.newObjectNode();
        expectedResult.put("data", 123); // {"data": 123}
        Assertions.assertEquals(expectedResult, actualResult);
        Assertions.assertThrows(IllegalArgumentException.class, () -> JacksonUtil.toJsonNode(data)); // syntax exception due to missing quotes in the field name!
    }

    @Test
    public void failOnUnknownPropertiesMapperTest() {
        Asset asset = new Asset();
        asset.setId(new AssetId(UUID.randomUUID()));
        asset.setName("Test");
        asset.setType("type");
        String serializedAsset = JacksonUtil.toString(asset);
        JsonNode jsonNode = JacksonUtil.toJsonNode(serializedAsset);
        // case: add new field to serialized Asset string and check for backward compatibility with original Asset object
        Assertions.assertNotNull(jsonNode);
        ((ObjectNode) jsonNode).put("test", (String) null);
        serializedAsset = JacksonUtil.toString(jsonNode);
        // deserialize with FAIL_ON_UNKNOWN_PROPERTIES = false
        Asset result = JacksonUtil.fromString(serializedAsset, Asset.class, true);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(asset.getId(), result.getId());
        Assertions.assertEquals(asset.getName(), result.getName());
        Assertions.assertEquals(asset.getType(), result.getType());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "false", "\"", "\"\"", "\"This is a string with double quotes\"", "Path: /home/developer/test.txt",
            "First line\nSecond line\n\nFourth line", "Before\rAfter", "Tab\tSeparated\tValues", "Test\bbackspace", "[]",
            "[1, 2, 3]", "{\"key\": \"value\"}", "{\n\"temperature\": 25.5,\n\"humidity\": 50.2\n\"}", "Expression: (a + b) * c",
            "世界", "Україна", "\u1F1FA\u1F1E6", "🇺🇦"})
    public void toPlainTextTest(String original) {
         String serialized = JacksonUtil.toString(original);
        Assertions.assertNotNull(serialized);
        Assertions.assertEquals(original, JacksonUtil.toPlainText(serialized));
    }

    @Test
    public void optionalMappingJDK8ModuleTest() {
        // To address the issue: Java 8 optional type `java.util.Optional` not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jdk8" to enable handling
        assertThat(JacksonUtil.writeValueAsString(Optional.of("hello"))).isEqualTo("\"hello\"");
        assertThat(JacksonUtil.writeValueAsString(List.of(Optional.of("abc")))).isEqualTo("[\"abc\"]");
        assertThat(JacksonUtil.writeValueAsString(Set.of(Optional.empty()))).isEqualTo("[null]");
    }

}