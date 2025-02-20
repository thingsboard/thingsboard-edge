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
package org.thingsboard.server.common.data.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema
public class CalculatedFieldDebugEventFilter extends DebugEventFilter {

    @Schema(description = "String value representing the entity id in the event body", example = "57b6bafe-d600-423c-9267-fe31e5218986")
    protected String entityId;
    @Schema(description = "String value representing the entity type", allowableValues = "DEVICE")
    protected String entityType;
    @Schema(description = "String value representing the message id in the rule engine", example = "dcf44612-2ce4-4e5d-b462-ebb9c5628228")
    protected String msgId;
    @Schema(description = "String value representing the message type", example = "POST_TELEMETRY_REQUEST")
    protected String msgType;
    @Schema(description = "String value representing the arguments that were used in the calculation performed",
            example = "{\"x\":{\"ts\":1739432016629,\"value\":20},\"y\":{\"ts\":1739429717656,\"value\":12}}")
    protected String arguments;
    @Schema(description = "String value representing the result of a calculation",
            example = "{\"x + y\":54}")
    protected String result;


    @Override
    public EventType getEventType() {
        return EventType.DEBUG_CALCULATED_FIELD;
    }

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(entityId) || !StringUtils.isEmpty(entityType)
                || !StringUtils.isEmpty(msgId) || !StringUtils.isEmpty(msgType)
                || !StringUtils.isEmpty(arguments) || !StringUtils.isEmpty(result);
    }

}
