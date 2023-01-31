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
package org.thingsboard.rule.engine.analytics.incoming;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.rule.engine.analytics.incoming.state.StatePersistPolicy;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.concurrent.TimeUnit;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbSimpleAggMsgNodeConfiguration extends TbAbstractLatestNodeConfiguration {

    private String mathFunction;

    private AggIntervalType aggIntervalType;
    private String timeZoneId;
    //For Static Intervals
    private String aggIntervalTimeUnit;
    private int aggIntervalValue;

    private boolean autoCreateIntervals;

    private String intervalPersistencePolicy;
    private String intervalCheckTimeUnit;
    private int intervalCheckValue;

    private String inputValueKey;
    private String outputValueKey;

    private String statePersistencePolicy;
    private String statePersistenceTimeUnit;
    private int statePersistenceValue;

    @Override
    public TbSimpleAggMsgNodeConfiguration defaultConfiguration() {
        TbSimpleAggMsgNodeConfiguration configuration = new TbSimpleAggMsgNodeConfiguration();

        configuration.setMathFunction(MathFunction.AVG.name());
        configuration.setAggIntervalType(AggIntervalType.HOUR);
        configuration.setAggIntervalTimeUnit(TimeUnit.HOURS.name());
        configuration.setAggIntervalValue(1);

        configuration.setAutoCreateIntervals(false);

        configuration.setParentEntitiesQuery(new ParentEntitiesGroup());

        configuration.setPeriodTimeUnit(TimeUnit.MINUTES);
        configuration.setPeriodValue(5);

        configuration.setIntervalPersistencePolicy(IntervalPersistPolicy.ON_EACH_CHECK_AFTER_INTERVAL_END.name());
        configuration.setIntervalCheckTimeUnit(TimeUnit.MINUTES.name());
        configuration.setIntervalCheckValue(1);

        configuration.setInputValueKey("temperature");
        configuration.setOutputValueKey("avgHourlyTemperature");

        configuration.setStatePersistencePolicy(StatePersistPolicy.ON_EACH_CHANGE.name());
        configuration.setStatePersistenceTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStatePersistenceValue(1);
        configuration.setOutMsgType(SessionMsgType.POST_TELEMETRY_REQUEST.name());

        return configuration;
    }

}
