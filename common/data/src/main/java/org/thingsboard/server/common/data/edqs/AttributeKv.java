/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.edqs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttributeKv implements EdqsObject {

    private EntityId entityId;
    private AttributeScope scope;
    private String key;
    private Long version;

    private DataPoint dataPoint; // optional (on deletion)

    private Long lastUpdateTs; // only for serialization
    private KvEntry value; // only for serialization

    public AttributeKv(EntityId entityId, AttributeScope scope, AttributeKvEntry attributeKvEntry, long version) {
        this.entityId = entityId;
        this.scope = scope;
        this.key = attributeKvEntry.getKey();
        this.version = version;
        this.lastUpdateTs = attributeKvEntry.getLastUpdateTs();
        this.value = attributeKvEntry;
    }

    public AttributeKv(EntityId entityId, AttributeScope scope, String key, long version) {
        this.entityId = entityId;
        this.scope = scope;
        this.key = key;
        this.version = version;
    }

    @Override
    public String key() {
        return "a_" + entityId + "_" + scope + "_" + key;
    }

    @Override
    public Long version() {
        return version;
    }

    @Override
    public ObjectType type() {
        return ObjectType.ATTRIBUTE_KV;
    }

}
