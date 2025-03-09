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
package org.thingsboard.server.common.data.audit;

import lombok.Getter;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.Optional;

public enum ActionType {

    ADDED(TbMsgType.ENTITY_CREATED), // log entity
    DELETED(TbMsgType.ENTITY_DELETED), // log string id
    UPDATED(TbMsgType.ENTITY_UPDATED), // log entity
    ATTRIBUTES_UPDATED(TbMsgType.ATTRIBUTES_UPDATED), // log attributes/values
    ATTRIBUTES_DELETED(TbMsgType.ATTRIBUTES_DELETED), // log attributes
    TIMESERIES_UPDATED(TbMsgType.TIMESERIES_UPDATED), // log timeseries update
    TIMESERIES_DELETED(TbMsgType.TIMESERIES_DELETED), // log timeseries
    RPC_CALL, // log method and params
    CREDENTIALS_UPDATED, // log new credentials
    ASSIGNED_TO_CUSTOMER(TbMsgType.ENTITY_ASSIGNED), // log customer name
    UNASSIGNED_FROM_CUSTOMER(TbMsgType.ENTITY_UNASSIGNED), // log customer name
    CHANGE_OWNER(TbMsgType.OWNER_CHANGED), // log customer name
    ACTIVATED, // log string id
    SUSPENDED, // log string id
    CREDENTIALS_READ(true), // log device id
    ATTRIBUTES_READ(true), // log attributes
    RELATION_ADD_OR_UPDATE(TbMsgType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(TbMsgType.RELATION_DELETED),
    RELATIONS_DELETED(TbMsgType.RELATIONS_DELETED),
    ALARM_ACK(TbMsgType.ALARM_ACK, true),
    ALARM_CLEAR(TbMsgType.ALARM_CLEAR, true),
    ALARM_DELETE(TbMsgType.ALARM_DELETE, true),
    ALARM_ASSIGNED(TbMsgType.ALARM_ASSIGNED, true),
    ALARM_UNASSIGNED(TbMsgType.ALARM_UNASSIGNED, true),
    ADDED_TO_ENTITY_GROUP(TbMsgType.ADDED_TO_ENTITY_GROUP), // log entity group name
    REMOVED_FROM_ENTITY_GROUP(TbMsgType.REMOVED_FROM_ENTITY_GROUP), // log entity group name
    REST_API_RULE_ENGINE_CALL, // log call to rule engine from REST API
    MADE_PUBLIC, // log entity group name
    MADE_PRIVATE, // log entity group name
    LOGIN,
    LOGOUT,
    LOCKOUT,
    ASSIGNED_FROM_TENANT(TbMsgType.ENTITY_ASSIGNED_FROM_TENANT),
    ASSIGNED_TO_TENANT(TbMsgType.ENTITY_ASSIGNED_TO_TENANT),
    PROVISION_SUCCESS(TbMsgType.PROVISION_SUCCESS),
    PROVISION_FAILURE(TbMsgType.PROVISION_FAILURE),
    ASSIGNED_TO_EDGE(TbMsgType.ENTITY_ASSIGNED_TO_EDGE), // log edge name
    UNASSIGNED_FROM_EDGE(TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE),
    ADDED_COMMENT(TbMsgType.COMMENT_CREATED),
    UPDATED_COMMENT(TbMsgType.COMMENT_UPDATED),
    DELETED_COMMENT,
    SMS_SENT;

    @Getter
    private final boolean read;

    private final TbMsgType ruleEngineMsgType;

    @Getter
    private final boolean alarmAction;

    ActionType() {
        this(false, null, false);
    }

    ActionType(boolean read) {
        this(read, null, false);
    }

    ActionType(TbMsgType ruleEngineMsgType) {
        this(false, ruleEngineMsgType, false);
    }

    ActionType(TbMsgType ruleEngineMsgType, boolean isAlarmAction) {
        this(false, ruleEngineMsgType, isAlarmAction);
    }

    ActionType(boolean read, TbMsgType ruleEngineMsgType, boolean alarmAction) {
        this.read = read;
        this.ruleEngineMsgType = ruleEngineMsgType;
        this.alarmAction = alarmAction;
    }

    public Optional<TbMsgType> getRuleEngineMsgType() {
        return Optional.ofNullable(ruleEngineMsgType);
    }

}
