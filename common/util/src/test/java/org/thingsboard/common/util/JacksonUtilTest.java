/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;

import java.util.UUID;

public class JacksonUtilTest {

    @Test
    public void allow_unquoted_field_mapper_test() {
        String data = "{data: 123}";
        JsonNode actualResult = JacksonUtil.toJsonNode(data, JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER); // should be: {"data": 123}
        ObjectNode expectedResult = JacksonUtil.newObjectNode();
        expectedResult.put("data", 123); // {"data": 123}
        Assert.assertEquals(expectedResult, actualResult);
        Assert.assertThrows(IllegalArgumentException.class, () -> JacksonUtil.toJsonNode(data)); // syntax exception due to missing quotes in the field name!
    }

    @Test
    public void fail_on_unknown_properties_edge_mapper_test() throws JsonProcessingException {
        Asset asset = new Asset();
        asset.setId(new AssetId(UUID.randomUUID()));
        asset.setName("Test");
        asset.setType("type");
        String serializedAsset = JacksonUtil.toString(asset);
        JsonNode jsonNode = JacksonUtil.EDGE_OBJECT_MAPPER.readTree(serializedAsset);
        // case: add new field to serialized Asset string and check for backward compatibility with original Asset object
        ((ObjectNode) jsonNode).put("test", (String) null);
        serializedAsset = JacksonUtil.toString(jsonNode);
        // deserialize with FAIL_ON_UNKNOWN_PROPERTIES = false
        Asset result = JacksonUtil.fromEdgeString(serializedAsset, Asset.class);
        Assert.assertNotNull(result);
        Assert.assertEquals(asset.getId(), result.getId());
        Assert.assertEquals(asset.getName(), result.getName());
        Assert.assertEquals(asset.getType(), result.getType());
    }

}