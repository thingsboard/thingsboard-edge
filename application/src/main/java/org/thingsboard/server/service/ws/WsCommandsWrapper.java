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
package org.thingsboard.server.service.ws;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsUnsubCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.GetHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.TimeseriesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusUnsubscribeCmd;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsCommandsWrapper {

    private AuthCmd authCmd;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @Type(name = "ATTRIBUTES", value = AttributesSubscriptionCmd.class),
            @Type(name = "TIMESERIES", value = TimeseriesSubscriptionCmd.class),
            @Type(name = "TIMESERIES_HISTORY", value = GetHistoryCmd.class),
            @Type(name = "ENTITY_DATA", value = EntityDataCmd.class),
            @Type(name = "ENTITY_COUNT", value = EntityCountCmd.class),
            @Type(name = "ALARM_DATA", value = AlarmDataCmd.class),
            @Type(name = "ALARM_COUNT", value = AlarmCountCmd.class),
            @Type(name = "ALARM_STATUS", value = AlarmStatusCmd.class),
            @Type(name = "NOTIFICATIONS", value = NotificationsSubCmd.class),
            @Type(name = "NOTIFICATIONS_COUNT", value = NotificationsCountSubCmd.class),
            @Type(name = "MARK_NOTIFICATIONS_AS_READ", value = MarkNotificationsAsReadCmd.class),
            @Type(name = "MARK_ALL_NOTIFICATIONS_AS_READ", value = MarkAllNotificationsAsReadCmd.class),
            @Type(name = "ALARM_DATA_UNSUBSCRIBE", value = AlarmDataUnsubscribeCmd.class),
            @Type(name = "ALARM_COUNT_UNSUBSCRIBE", value = AlarmCountUnsubscribeCmd.class),
            @Type(name = "ENTITY_DATA_UNSUBSCRIBE", value = EntityDataUnsubscribeCmd.class),
            @Type(name = "ENTITY_COUNT_UNSUBSCRIBE", value = EntityCountUnsubscribeCmd.class),
            @Type(name = "NOTIFICATIONS_UNSUBSCRIBE", value = NotificationsUnsubCmd.class),
            @Type(name = "ALARM_STATUS_UNSUBSCRIBE", value = AlarmStatusUnsubscribeCmd.class),
    })
    private List<WsCmd> cmds;

}
