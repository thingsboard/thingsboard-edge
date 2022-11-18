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
package org.thingsboard.server.service.solutions.data.values;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.service.solutions.data.definition.TelemetryProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.getMultiplier;
import static org.thingsboard.server.service.solutions.data.values.GeneratorTools.randomDouble;

public class NaturalTelemetryGenerator extends TelemetryGenerator {

    private final NaturalValueStrategyDefinition strategy;
    @Getter @Setter
    private double value;
    private boolean isIncrement;
    private double lowValue;
    private double highValue;

    public NaturalTelemetryGenerator(TelemetryProfile telemetryProfile) {
        super(telemetryProfile);
        this.strategy = (NaturalValueStrategyDefinition) telemetryProfile.getValueStrategy();
        this.value = getRandomStartValue();
        this.lowValue = getRandomLowValue();
        this.highValue = getRandomHighValue();
        isIncrement = !strategy.isDecrementOnStart();
    }

    @Override
    public void addValue(long ts, ObjectNode values) {

        double multiplier = getMultiplier(ts, strategy.getHolidayMultiplier(), strategy.getWorkHoursMultiplier(), strategy.getNightHoursMultiplier());

        if (isIncrement) {
            double step = randomDouble(strategy.getMinIncrement(), strategy.getMaxIncrement());
            value += step * multiplier;
            if (value > highValue) {
                value = highValue;
                highValue = getRandomHighValue();
                isIncrement = false;
            }
        } else {
            double step = randomDouble(strategy.getMinDecrement(), strategy.getMaxDecrement());
            value -= step * multiplier;
            if (value < lowValue) {
                value = lowValue;
                lowValue = getRandomLowValue();
                isIncrement = true;
            }
        }

        put(values, value);
    }

    private void put(ObjectNode values, double value) {
        if (strategy.getPrecision() == 0) {
            values.put(key, (int) value);
        } else {
            values.put(key, BigDecimal.valueOf(value)
                    .setScale(strategy.getPrecision(), RoundingMode.HALF_UP)
                    .doubleValue());
        }
    }

    public double getRandomStartValue() {
        return randomDouble(strategy.getMinStartValue(), strategy.getMaxStartValue());
    }

    public double getRandomLowValue() {
        return randomDouble(strategy.getMinLowValue(), strategy.getMaxLowValue());
    }

    public double getRandomHighValue() {
        return randomDouble(strategy.getMinHighValue(), strategy.getMaxHighValue());
    }
}
