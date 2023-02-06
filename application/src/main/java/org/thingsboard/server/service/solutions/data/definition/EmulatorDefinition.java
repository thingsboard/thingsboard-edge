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
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class EmulatorDefinition {
    private String name;
    private String extendz;
    private String clazz;
    private int publishPeriodInDays;
    private int publishFrequencyInSeconds;
    private int publishPauseInMillis;
    private long activityPeriodInMillis;
    private List<TelemetryProfile> telemetryProfiles = Collections.emptyList();

    public void enrich(EmulatorDefinition parent) {
        if (StringUtils.isEmpty(clazz)) {
            clazz = parent.getClazz();
        }
        if (publishPeriodInDays == 0) {
            publishPeriodInDays = parent.getPublishPeriodInDays();
        }
        if (publishFrequencyInSeconds == 0) {
            publishFrequencyInSeconds = parent.getPublishFrequencyInSeconds();
        }
        if (publishPauseInMillis == 0) {
            publishPauseInMillis = parent.getPublishPauseInMillis();
        }
        if (activityPeriodInMillis == 0L) {
            activityPeriodInMillis = parent.getActivityPeriodInMillis();
        }
        var profilesMap = telemetryProfiles.stream().collect(Collectors.toMap(TelemetryProfile::getKey, Function.identity()));
        parent.getTelemetryProfiles().forEach(tp -> profilesMap.putIfAbsent(tp.getKey(), tp));
        telemetryProfiles = new ArrayList<>(profilesMap.values());
    }
}
