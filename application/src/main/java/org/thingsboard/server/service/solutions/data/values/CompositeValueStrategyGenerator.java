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
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

public class CompositeValueStrategyGenerator extends TelemetryGenerator {

    TelemetryGenerator defaultGenerator;
    TelemetryGenerator whGenerator;
    TelemetryGenerator nhGenerator;
    TelemetryGenerator hhGenerator;

    public CompositeValueStrategyGenerator(TelemetryProfile tp) {
        super(tp);
        var def = (CompositeValueStrategyDefinition) tp.getValueStrategy();
        defaultGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getDefaultHours()));
        if (def.getWorkHours() != null) {
            whGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getWorkHours()));
        }
        if (def.getNightHours() != null) {
            nhGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getNightHours()));
        }
        if (def.getHolidayHours() != null) {
            hhGenerator = TelemetryGeneratorFactory.create(new TelemetryProfile(tp.getKey(), def.getHolidayHours()));
        }
    }

    @Override
    public void addValue(long ts, ObjectNode values) {
        if (hhGenerator != null && GeneratorTools.isHoliday(ts)) {
            hhGenerator.addValue(ts, values);
        } else if (nhGenerator != null && GeneratorTools.isNightHour(ts)) {
            nhGenerator.addValue(ts, values);
        } else if (whGenerator != null && GeneratorTools.isWorkHour(ts)) {
            whGenerator.addValue(ts, values);
        } else {
            defaultGenerator.addValue(ts, values);
        }
    }
}
