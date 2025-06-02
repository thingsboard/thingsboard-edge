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
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.UUID;

@Data
@SuperBuilder
public class AbstractEntityFields implements EntityFields {

    private UUID id;
    private long createdTime;
    private UUID tenantId;
    private UUID customerId;
    private String name;
    private Long version;

    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId, UUID customerId, String name, Long version) {
        this.id = id;
        this.createdTime = createdTime;
        this.tenantId = tenantId;
        this.customerId = checkId(customerId);
        this.name = name;
        this.version = version;
    }

    public AbstractEntityFields() {
    }

    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId, String name, Long version) {
        this(id, createdTime, tenantId, null, name, version);
    }

    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId, UUID customerId, Long version) {
        this(id, createdTime, tenantId, customerId, null, version);

    }

    public AbstractEntityFields(UUID id, long createdTime, String name, Long version) {
        this(id, createdTime, null, name, version);
    }


    public AbstractEntityFields(UUID id, long createdTime, UUID tenantId) {
        this(id, createdTime, tenantId, null, null, null);
    }

    protected UUID checkId(UUID id) {
        return id == null || id.equals(EntityId.NULL_UUID) ? null : id;
    }

    @Override
    public UUID getCustomerId() {
        return checkId(customerId);
    }

}
