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

import lombok.Getter;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.Optional;

public enum ActionType {

    ADDED(false, TbMsgType.ENTITY_CREATED), // log entity
    DELETED(false, TbMsgType.ENTITY_DELETED), // log string id
    UPDATED(false, TbMsgType.ENTITY_UPDATED), // log entity
    ATTRIBUTES_UPDATED(false, TbMsgType.ATTRIBUTES_UPDATED), // log attributes/values
    ATTRIBUTES_DELETED(false, TbMsgType.ATTRIBUTES_DELETED), // log attributes
    TIMESERIES_UPDATED(false, TbMsgType.TIMESERIES_UPDATED), // log timeseries update
    TIMESERIES_DELETED(false, TbMsgType.TIMESERIES_DELETED), // log timeseries
    RPC_CALL(false, null), // log method and params
    CREDENTIALS_UPDATED(false, null), // log new credentials
    ASSIGNED_TO_CUSTOMER(false, TbMsgType.ENTITY_ASSIGNED), // log customer name
    UNASSIGNED_FROM_CUSTOMER(false, TbMsgType.ENTITY_UNASSIGNED), // log customer name
    CHANGE_OWNER(false, TbMsgType.OWNER_CHANGED), // log customer name
    ACTIVATED(false, null), // log string id
    SUSPENDED(false, null), // log string id
    CREDENTIALS_READ(true, null), // log device id
    ATTRIBUTES_READ(true, null), // log attributes
    RELATION_ADD_OR_UPDATE(false, TbMsgType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(false, TbMsgType.RELATION_DELETED),
    RELATIONS_DELETED(false, TbMsgType.RELATIONS_DELETED),
    ALARM_ACK(false, TbMsgType.ALARM_ACK),
    ALARM_CLEAR(false, TbMsgType.ALARM_CLEAR),
    ALARM_DELETE(false, TbMsgType.ALARM_DELETE),
    ALARM_ASSIGNED(false, TbMsgType.ALARM_ASSIGNED),
    ALARM_UNASSIGNED(false, TbMsgType.ALARM_UNASSIGNED),
    ADDED_TO_ENTITY_GROUP(false, TbMsgType.ADDED_TO_ENTITY_GROUP), // log entity group name
    REMOVED_FROM_ENTITY_GROUP(false, TbMsgType.REMOVED_FROM_ENTITY_GROUP), // log entity group name
    REST_API_RULE_ENGINE_CALL(false, null), // log call to rule engine from REST API
    MADE_PUBLIC(false, null), // log entity group name
    MADE_PRIVATE(false, null), // log entity group name
    LOGIN(false, null),
    LOGOUT(false, null),
    LOCKOUT(false, null),
    ASSIGNED_FROM_TENANT(false, TbMsgType.ENTITY_ASSIGNED_FROM_TENANT),
    ASSIGNED_TO_TENANT(false, TbMsgType.ENTITY_ASSIGNED_TO_TENANT),
    PROVISION_SUCCESS(false, TbMsgType.PROVISION_SUCCESS),
    PROVISION_FAILURE(false, TbMsgType.PROVISION_FAILURE),
    ASSIGNED_TO_EDGE(false, TbMsgType.ENTITY_ASSIGNED_TO_EDGE), // log edge name
    UNASSIGNED_FROM_EDGE(false, TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE),
    ADDED_COMMENT(false, TbMsgType.COMMENT_CREATED),
    UPDATED_COMMENT(false, TbMsgType.COMMENT_UPDATED),
    DELETED_COMMENT(false, null),
    SMS_SENT(false, null);

    @Getter
    private final boolean isRead;

    private final TbMsgType ruleEngineMsgType;

    ActionType(boolean isRead, TbMsgType ruleEngineMsgType) {
        this.isRead = isRead;
        this.ruleEngineMsgType = ruleEngineMsgType;
    }

    public Optional<TbMsgType> getRuleEngineMsgType() {
        return Optional.ofNullable(ruleEngineMsgType);
    }

}
