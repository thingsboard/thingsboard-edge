/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.common.data.relation;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

/**
 * Created by ashvayka on 03.05.17.
 */
@Data
@AllArgsConstructor
public class RelationsSearchParameters {

    private UUID rootId;
    private EntityType rootType;
    private EntitySearchDirection direction;
    private RelationTypeGroup relationTypeGroup;
    private int maxLevel = 1;

    public RelationsSearchParameters(EntityId entityId, EntitySearchDirection direction, int maxLevel) {
        this(entityId, direction, maxLevel, RelationTypeGroup.COMMON);
    }

    public RelationsSearchParameters(EntityId entityId, EntitySearchDirection direction, int maxLevel, RelationTypeGroup relationTypeGroup) {
        this.rootId = entityId.getId();
        this.rootType = entityId.getEntityType();
        this.direction = direction;
        this.maxLevel = maxLevel;
        this.relationTypeGroup = relationTypeGroup;
    }

    public EntityId getEntityId() {
        return EntityIdFactory.getByTypeAndUuid(rootType, rootId);
    }
}
