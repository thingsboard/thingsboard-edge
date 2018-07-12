/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

@Getter
public enum ActionType {
    ADDED(false), // log entity
    DELETED(false), // log string id
    UPDATED(false), // log entity
    ATTRIBUTES_UPDATED(false), // log attributes/values
    ATTRIBUTES_DELETED(false), // log attributes
    RPC_CALL(false), // log method and params
    CREDENTIALS_UPDATED(false), // log new credentials
    ASSIGNED_TO_CUSTOMER(false), // log customer name
    UNASSIGNED_FROM_CUSTOMER(false), // log customer name
    ACTIVATED(false), // log string id
    SUSPENDED(false), // log string id
    CREDENTIALS_READ(true), // log device id
    ATTRIBUTES_READ(true), // log attributes
    ADDED_TO_ENTITY_GROUP(false), // log entity group name
    REMOVED_FROM_ENTITY_GROUP(false); // log entity group name

    private final boolean isRead;

    ActionType(boolean isRead) {
        this.isRead = isRead;
    }
}
