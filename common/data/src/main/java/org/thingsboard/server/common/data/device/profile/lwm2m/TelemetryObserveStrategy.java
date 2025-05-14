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

import lombok.Getter;

public enum TelemetryObserveStrategy {

    SINGLE("One resource equals one single observe request", 0),
    COMPOSITE_ALL("All resources in one composite observe request", 1),
    COMPOSITE_BY_OBJECT("Grouped composite observe requests by object", 2);

    @Getter
    private final String description;

    @Getter
    private final int id;

    TelemetryObserveStrategy(String description, int id) {
        this.description = description;
        this.id = id;
    }

    public static TelemetryObserveStrategy fromDescription(String description) {
        for (TelemetryObserveStrategy strategy : values()) {
            if (strategy.description.equalsIgnoreCase(description)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown TelemetryObserveStrategy id: " + description);
    }

    public static TelemetryObserveStrategy fromId(int id) {
        for (TelemetryObserveStrategy strategy : values()) {
            if (strategy.id == id) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown TelemetryObserveStrategy id: " + id);
    }

    @Override
    public String toString() {
        return name() + " (" + id + "): " + description;
    }
}
