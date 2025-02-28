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
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class ApiUsageStateFields extends AbstractEntityFields {

    private EntityId entityId;
    private ApiUsageStateValue transportState;
    private ApiUsageStateValue dbStorageState;
    private ApiUsageStateValue reExecState;
    private ApiUsageStateValue jsExecState;
    private ApiUsageStateValue tbelExecState;
    private ApiUsageStateValue emailExecState;
    private ApiUsageStateValue smsExecState;
    private ApiUsageStateValue alarmExecState;

    public ApiUsageStateFields(UUID id, long createdTime, UUID tenantId, UUID entityId, String entityType, ApiUsageStateValue transportState, ApiUsageStateValue dbStorageState,
                               ApiUsageStateValue reExecState, ApiUsageStateValue jsExecState, ApiUsageStateValue tbelExecState,
                               ApiUsageStateValue emailExecState, ApiUsageStateValue smsExecState, ApiUsageStateValue alarmExecState,
                               Long version) {
        super(id, createdTime, tenantId, null, null, version);
        this.entityId = (entityType != null && entityId != null) ? EntityIdFactory.getByTypeAndUuid(entityType, entityId) : null;
        this.transportState = transportState;
        this.dbStorageState = dbStorageState;
        this.reExecState = reExecState;
        this.jsExecState = jsExecState;
        this.tbelExecState = tbelExecState;
        this.emailExecState = emailExecState;
        this.smsExecState = smsExecState;
        this.alarmExecState = alarmExecState;
    }
}
