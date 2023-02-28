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
package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel
public class RuleNodeDebugEventFilter extends DebugEventFilter {

    @ApiModelProperty(position = 2, value = "String value representing msg direction type (incoming to entity or outcoming from entity)", allowableValues = "IN, OUT")
    protected String msgDirectionType;
    @ApiModelProperty(position = 3, value = "String value representing the entity id in the event body (originator of the message)", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    protected String entityId;
    @ApiModelProperty(position = 4, value = "String value representing the entity type", allowableValues = "DEVICE")
    protected String entityType;
    @ApiModelProperty(position = 5, value = "String value representing the message id in the rule engine", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    protected String msgId;
    @ApiModelProperty(position = 6, value = "String value representing the message type", example = "POST_TELEMETRY_REQUEST")
    protected String msgType;
    @ApiModelProperty(position = 7, value = "String value representing the type of message routing", example = "Success")
    protected String relationType;
    @ApiModelProperty(position = 8, value = "The case insensitive 'contains' filter based on data (key and value) for the message.", example = "humidity")
    protected String dataSearch;
    @ApiModelProperty(position = 9, value = "The case insensitive 'contains' filter based on metadata (key and value) for the message.", example = "deviceName")
    protected String metadataSearch;

    @Override
    public EventType getEventType() {
        return EventType.DEBUG_RULE_NODE;
    }

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(msgDirectionType) || !StringUtils.isEmpty(entityId)
                || !StringUtils.isEmpty(entityType) || !StringUtils.isEmpty(msgId) || !StringUtils.isEmpty(msgType) ||
                !StringUtils.isEmpty(relationType) || !StringUtils.isEmpty(dataSearch) || !StringUtils.isEmpty(metadataSearch);
    }
}
