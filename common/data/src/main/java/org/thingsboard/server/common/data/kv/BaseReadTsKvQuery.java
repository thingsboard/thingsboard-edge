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
package org.thingsboard.server.common.data.kv;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZoneId;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseReadTsKvQuery extends BaseTsKvQuery implements ReadTsKvQuery {

    private final AggregationParams aggParameters;
    private final int limit;
    private final String order;

    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation) {
        this(key, startTs, endTs, interval, limit, aggregation, "DESC");
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation, String descOrder) {
        this(key, startTs, endTs, AggregationParams.of(aggregation, IntervalType.MILLISECONDS, ZoneId.systemDefault(), interval), limit, descOrder);
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, AggregationParams parameters, int limit) {
        this(key, startTs, endTs, parameters, limit, "DESC");
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, AggregationParams parameters, int limit, String order) {
        super(key, startTs, endTs);
        this.aggParameters = parameters;
        this.limit = limit;
        this.order = order;
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs) {
        this(key, startTs, endTs, AggregationParams.milliseconds(Aggregation.AVG, endTs - startTs), 1, "DESC");
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, int limit, String order) {
        this(key, startTs, endTs, AggregationParams.none(), limit, order);
    }

    public BaseReadTsKvQuery(ReadTsKvQuery query, long startTs, long endTs) {
        super(query.getId(), query.getKey(), startTs, endTs);
        this.aggParameters = query.getAggParameters();
        this.limit = query.getLimit();
        this.order = query.getOrder();
    }
}
