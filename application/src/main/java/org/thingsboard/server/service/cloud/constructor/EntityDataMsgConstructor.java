/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.cloud.constructor;

import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.gen.edge.EntityDataProto;

@Component
@Slf4j
public class EntityDataMsgConstructor {

    public EntityDataProto constructEntityDataMsg(EntityId entityId, ActionType actionType, JsonElement entityData, long ts) {
        EntityDataProto.Builder builder = EntityDataProto.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name());
        switch (actionType) {
            case TIMESERIES_UPDATED:
                try {
                    builder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(entityData, ts));
                } catch (Exception e) {
                    log.warn("Can't convert to telemetry proto, entityData [{}]", entityData, e);
                }
                break;
            case ATTRIBUTES_UPDATED:
                try {
                    builder.setPostAttributesMsg(JsonConverter.convertToAttributesProto(entityData));
                } catch (Exception e) {
                    log.warn("Can't convert to attributes proto, entityData [{}]", entityData, e);
                }
                break;
            // TODO: voba - add support for attribute delete
            // case ATTRIBUTES_DELETED:
        }
        return builder.build();
    }

}
