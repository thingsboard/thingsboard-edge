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
package org.thingsboard.server.common.data.audit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.audit.ActionType.ACTIVATED;
import static org.thingsboard.server.common.data.audit.ActionType.ATTRIBUTES_READ;
import static org.thingsboard.server.common.data.audit.ActionType.CREDENTIALS_READ;
import static org.thingsboard.server.common.data.audit.ActionType.CREDENTIALS_UPDATED;
import static org.thingsboard.server.common.data.audit.ActionType.DELETED_COMMENT;
import static org.thingsboard.server.common.data.audit.ActionType.LOCKOUT;
import static org.thingsboard.server.common.data.audit.ActionType.LOGIN;
import static org.thingsboard.server.common.data.audit.ActionType.LOGOUT;
import static org.thingsboard.server.common.data.audit.ActionType.MADE_PRIVATE;
import static org.thingsboard.server.common.data.audit.ActionType.MADE_PUBLIC;
import static org.thingsboard.server.common.data.audit.ActionType.REST_API_RULE_ENGINE_CALL;
import static org.thingsboard.server.common.data.audit.ActionType.RPC_CALL;
import static org.thingsboard.server.common.data.audit.ActionType.SMS_SENT;
import static org.thingsboard.server.common.data.audit.ActionType.SUSPENDED;

class ActionTypeTest {

    private static final List<ActionType> typesWithNullRuleEngineMsgType = List.of(
            RPC_CALL,
            CREDENTIALS_UPDATED,
            ACTIVATED,
            SUSPENDED,
            CREDENTIALS_READ,
            ATTRIBUTES_READ,
            LOGIN,
            LOGOUT,
            LOCKOUT,
            DELETED_COMMENT,
            SMS_SENT,
            REST_API_RULE_ENGINE_CALL,
            MADE_PUBLIC,
            MADE_PRIVATE
    );

    // backward-compatibility tests

    @Test
    void getRuleEngineMsgTypeTest() {
        var types = ActionType.values();
        for (var type : types) {
            if (typesWithNullRuleEngineMsgType.contains(type)) {
                assertThat(type.getRuleEngineMsgType()).isEmpty();
            }
        }
    }

}
