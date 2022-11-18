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
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
public class TbAggLatestTelemetryNodeV2Configuration implements NodeConfiguration {

    private static final int MIN_DEDUPLICATION_IN_SEC = 10;

    private String queueName;
    private String outMsgType;

    private EntitySearchDirection direction;
    private String relationType;
    private long deduplicationInSec;
    private List<AggLatestMapping> aggMappings;

    @Override
    public TbAggLatestTelemetryNodeV2Configuration defaultConfiguration() {
        TbAggLatestTelemetryNodeV2Configuration configuration = new TbAggLatestTelemetryNodeV2Configuration();

        List<AggLatestMapping> aggMappings = new ArrayList<>();
        AggLatestMapping aggMapping = new AggLatestMapping();
        aggMapping.setSource("temperature");
        aggMapping.setSourceScope("LATEST_TELEMETRY");
        aggMapping.setAggFunction(MathFunction.AVG);
        aggMapping.setDefaultValue(0);
        aggMapping.setTarget("latestAvgTemperature");
        aggMappings.add(aggMapping);

        configuration.setAggMappings(aggMappings);
        configuration.setOutMsgType(SessionMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }

    public long getDeduplicationInSec() {
        return Math.max(MIN_DEDUPLICATION_IN_SEC, deduplicationInSec);
    }
}
