/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.util.TbPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

@Slf4j
class TbMsgAttributesNodeTest {

    @Test
    void testUpgrade_fromVersion0() throws TbNodeException {
        final String updateAttributesOnValueChangeKey = "updateAttributesOnValueChange";
        TbMsgAttributesNode node = mock(TbMsgAttributesNode.class);
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        jsonNode.remove(updateAttributesOnValueChangeKey);
        assertThat(jsonNode.has(updateAttributesOnValueChangeKey)).as("pre condition has no " + updateAttributesOnValueChangeKey).isFalse();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isTrue();
        assertThat(resultNode.has(updateAttributesOnValueChangeKey)).as("upgrade result has key " + updateAttributesOnValueChangeKey).isTrue();
        assertThat(resultNode.get(updateAttributesOnValueChangeKey).asBoolean()).as("upgrade result value [false] for key " + updateAttributesOnValueChangeKey).isFalse();
    }

    @Test
    void testUpgrade_fromVersion0_alreadyHasUpdateAttributesOnValueChange() throws TbNodeException {
        final String updateAttributesOnValueChangeKey = "updateAttributesOnValueChange";
        TbMsgAttributesNode node = mock(TbMsgAttributesNode.class);
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        jsonNode.remove(updateAttributesOnValueChangeKey);
        jsonNode.put(updateAttributesOnValueChangeKey, true);
        assertThat(jsonNode.has(updateAttributesOnValueChangeKey)).as("pre condition has no " + updateAttributesOnValueChangeKey).isTrue();
        assertThat(jsonNode.get(updateAttributesOnValueChangeKey).asBoolean()).as("pre condition has [true] for key " + updateAttributesOnValueChangeKey).isTrue();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isFalse();
        assertThat(resultNode.has(updateAttributesOnValueChangeKey)).as("upgrade result has key " + updateAttributesOnValueChangeKey).isTrue();
        assertThat(resultNode.get(updateAttributesOnValueChangeKey).asBoolean()).as("upgrade result value [true] for key " + updateAttributesOnValueChangeKey).isTrue();
    }

}
