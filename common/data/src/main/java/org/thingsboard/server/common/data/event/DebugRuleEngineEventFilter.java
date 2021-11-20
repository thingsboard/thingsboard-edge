/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
public abstract class DebugRuleEngineEventFilter extends DebugEventFilter {

    @ApiModelProperty(position = 4, value = "String value representing msg direction type (incoming to entity or outcoming from entity)", allowableValues = "IN, OUT")
    private String msgDirectionType;
    @ApiModelProperty(position = 5, value = "The case insensitive 'contains' filter based on data (key and value) for the message.", example = "humidity")
    private String dataSearch;
    @ApiModelProperty(position = 6, value = "The case insensitive 'contains' filter based on metadata (key and value) for the message.", example = "deviceName")
    private String metadataSearch;
    @ApiModelProperty(position = 7, value = "String value representing the entity type", allowableValues = "DEVICE")
    private String entityName;
    @ApiModelProperty(position = 8, value = "String value representing the type of message routing", example = "Success")
    private String relationType;
    @ApiModelProperty(position = 9, value = "String value representing the entity id in the event body (originator of the message)", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    private String entityId;
    @ApiModelProperty(position = 10, value = "String value representing the message type", example = "POST_TELEMETRY_REQUEST")
    private String msgType;

    @Override
    public boolean hasFilterForJsonBody() {
        return super.hasFilterForJsonBody() || !StringUtils.isEmpty(msgDirectionType) || !StringUtils.isEmpty(dataSearch) || !StringUtils.isEmpty(metadataSearch)
                || !StringUtils.isEmpty(entityName) || !StringUtils.isEmpty(relationType) || !StringUtils.isEmpty(entityId) || !StringUtils.isEmpty(msgType);
    }

}
