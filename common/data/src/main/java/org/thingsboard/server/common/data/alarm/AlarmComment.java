/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@Builder
@AllArgsConstructor
public class AlarmComment extends BaseData<AlarmCommentId> implements HasName {
    @ApiModelProperty(position = 3, value = "JSON object with Alarm id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId alarmId;
    @ApiModelProperty(position = 4, value = "JSON object with User id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private UserId userId;
    @ApiModelProperty(position = 5, value = "Defines origination of comment", example = "System/Other", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @ApiModelProperty(position = 6, value = "JSON object with text of comment.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    private transient JsonNode comment;

    @ApiModelProperty(position = 1, value = "JSON object with the alarm comment Id. " +
            "Specify this field to update the alarm comment. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm." )
    @Override
    public AlarmCommentId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the alarm comment creation, in milliseconds", example = "1634058704567", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    public AlarmComment() {
        super();
    }

    public AlarmComment(AlarmCommentId id) {
        super(id);
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @ApiModelProperty(position = 5, required = true, value = "representing comment text", example = "Please take a look")
    public String getName() {
        return comment.toString();
    }

    public AlarmComment(AlarmComment alarmComment) {
        super(alarmComment.getId());
        this.createdTime = alarmComment.getCreatedTime();
        this.alarmId = alarmComment.getAlarmId();
        this.type = alarmComment.getType();
        this.comment = alarmComment.getComment();
        this.userId = alarmComment.getUserId();
    }
}
