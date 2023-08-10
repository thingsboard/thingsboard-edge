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
package org.thingsboard.server.common.data.msg;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_DELETE;
import static org.thingsboard.server.common.data.msg.TbMsgType.CUSTOM_OR_NA_TYPE;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DELAY_TIMEOUT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_PROFILE_PERIODIC_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_PROFILE_UPDATE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.DEVICE_UPDATE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_ASSIGNED_TO_EDGE;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE;
import static org.thingsboard.server.common.data.msg.TbMsgType.GENERATOR_NODE_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.MSG_COUNT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.OPC_UA_INT_FAILURE;
import static org.thingsboard.server.common.data.msg.TbMsgType.OPC_UA_INT_SUCCESS;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_FAILURE;
import static org.thingsboard.server.common.data.msg.TbMsgType.PROVISION_SUCCESS;
import static org.thingsboard.server.common.data.msg.TbMsgType.SEND_EMAIL;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_AGG_LATEST_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_ALARMS_COUNT_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_SIMPLE_AGG_ENTITIES_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_SIMPLE_AGG_PERSIST_SELF_MSG;
import static org.thingsboard.server.common.data.msg.TbMsgType.TB_SIMPLE_AGG_REPORT_SELF_MSG;

class TbMsgTypeTest {
    
    private static final List<TbMsgType> typesWithNullRuleNodeConnection = List.of(
            ALARM,
            ALARM_DELETE,
            ENTITY_ASSIGNED_TO_EDGE,
            ENTITY_UNASSIGNED_FROM_EDGE,
            PROVISION_FAILURE,
            PROVISION_SUCCESS,
            SEND_EMAIL,
            GENERATOR_NODE_SELF_MSG,
            DEVICE_PROFILE_PERIODIC_SELF_MSG,
            DEVICE_PROFILE_UPDATE_SELF_MSG,
            DEVICE_UPDATE_SELF_MSG,
            DEDUPLICATION_TIMEOUT_SELF_MSG,
            DELAY_TIMEOUT_SELF_MSG,
            MSG_COUNT_SELF_MSG,
            TB_AGG_LATEST_SELF_MSG,
            TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG,
            TB_ALARMS_COUNT_SELF_MSG,
            TB_SIMPLE_AGG_REPORT_SELF_MSG,
            TB_SIMPLE_AGG_PERSIST_SELF_MSG,
            TB_SIMPLE_AGG_ENTITIES_SELF_MSG,
            OPC_UA_INT_SUCCESS,
            OPC_UA_INT_FAILURE,
            CUSTOM_OR_NA_TYPE
    );

    // backward-compatibility tests

    @Test
    void getRuleNodeConnectionsTest() {
        var tbMsgTypes = TbMsgType.values();
        for (var type : tbMsgTypes) {
            if (typesWithNullRuleNodeConnection.contains(type)) {
                assertThat(type.getRuleNodeConnection()).isNull();
            } else {
                assertThat(type.getRuleNodeConnection()).isNotNull();
            }
        }
    }

    @Test
    void getRuleNodeConnectionOrElseOtherTest() {
        assertThat(TbMsgType.getRuleNodeConnectionOrElseOther(null))
                .isEqualTo(TbNodeConnectionType.OTHER);
        var tbMsgTypes = TbMsgType.values();
        for (var type : tbMsgTypes) {
            if (typesWithNullRuleNodeConnection.contains(type)) {
                assertThat(TbMsgType.getRuleNodeConnectionOrElseOther(type))
                        .isEqualTo(TbNodeConnectionType.OTHER);
            } else {
                assertThat(TbMsgType.getRuleNodeConnectionOrElseOther(type)).isNotNull()
                        .isNotEqualTo(TbNodeConnectionType.OTHER);
            }
        }
    }

    @Test
    void getCustomTypeTest() {
        var tbMsgTypes = TbMsgType.values();
        for (var type : tbMsgTypes) {
            if (type.equals(CUSTOM_OR_NA_TYPE)) {
                assertThat(type.isCustomType()).isTrue();
                continue;
            }
            assertThat(type.isCustomType()).isFalse();
        }
    }

}
