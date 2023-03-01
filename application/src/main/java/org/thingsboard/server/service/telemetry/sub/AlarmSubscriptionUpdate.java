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
package org.thingsboard.server.service.telemetry.sub;

import lombok.Getter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.AlarmData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class AlarmSubscriptionUpdate {

    @Getter
    private int subscriptionId;
    @Getter
    private int errorCode;
    @Getter
    private String errorMsg;
    @Getter
    private Alarm alarm;
    @Getter
    private boolean alarmDeleted;

    public AlarmSubscriptionUpdate(int subscriptionId, Alarm alarm) {
        this(subscriptionId, alarm, false);
    }

    public AlarmSubscriptionUpdate(int subscriptionId, Alarm alarm, boolean alarmDeleted) {
        super();
        this.subscriptionId = subscriptionId;
        this.alarm = alarm;
        this.alarmDeleted = alarmDeleted;
    }

    public AlarmSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode) {
        this(subscriptionId, errorCode, null);
    }

    public AlarmSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode, String errorMsg) {
        super();
        this.subscriptionId = subscriptionId;
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }

    @Override
    public String toString() {
        return "AlarmUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", alarm="
                + alarm + "]";
    }
}
