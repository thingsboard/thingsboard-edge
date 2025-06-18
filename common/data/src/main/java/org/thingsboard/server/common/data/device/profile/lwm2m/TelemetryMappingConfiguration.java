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
package org.thingsboard.server.common.data.device.profile.lwm2m;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class TelemetryMappingConfiguration implements Serializable {

    private static final long serialVersionUID = -7594999741305410419L;

    private Map<String, String> keyName;
    private Set<String> observe;
    private Set<String> attribute;
    private Set<String> telemetry;
    private Map<String, ObjectAttributes> attributeLwm2m;
    private TelemetryObserveStrategy observeStrategy;

    @JsonCreator
    public TelemetryMappingConfiguration(
            @JsonProperty("keyName") Map<String, String> keyName,
            @JsonProperty("observe") Set<String> observe,
            @JsonProperty("attribute") Set<String> attribute,
            @JsonProperty("telemetry") Set<String> telemetry,
            @JsonProperty("attributeLwm2m") Map<String, ObjectAttributes> attributeLwm2m,
            @JsonProperty("observeStrategy") TelemetryObserveStrategy observeStrategy) {

        this.keyName = keyName != null ? keyName : Collections.emptyMap();
        this.observe = observe != null ? observe : Collections.emptySet();
        this.attribute = attribute != null ? attribute : Collections.emptySet();
        this.telemetry = telemetry != null ? telemetry : Collections.emptySet();
        this.attributeLwm2m = attributeLwm2m != null ? attributeLwm2m : Collections.emptyMap();
        this.observeStrategy = observeStrategy != null ? observeStrategy : TelemetryObserveStrategy.SINGLE;
    }
}
