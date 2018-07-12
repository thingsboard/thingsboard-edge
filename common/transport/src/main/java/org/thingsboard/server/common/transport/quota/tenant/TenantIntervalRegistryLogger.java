/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.common.transport.quota.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.quota.inmemory.IntervalRegistryLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TenantIntervalRegistryLogger extends IntervalRegistryLogger {

    private final long logIntervalMin;

    public TenantIntervalRegistryLogger(@Value("${quota.rule.tenant.log.topSize}") int topSize,
                                        @Value("${quota.rule.tenant.log.intervalMin}") long logIntervalMin,
                                        TenantMsgsIntervalRegistry intervalRegistry) {
        super(topSize, logIntervalMin, intervalRegistry);
        this.logIntervalMin = logIntervalMin;
    }

    protected void log(Map<String, Long> top, int uniqHosts, long requestsCount) {
        long rps = requestsCount / TimeUnit.MINUTES.toSeconds(logIntervalMin);
        StringBuilder builder = new StringBuilder("Tenant Quota Statistic : ");
        builder.append("uniqTenants : ").append(uniqHosts).append("; ");
        builder.append("requestsCount : ").append(requestsCount).append("; ");
        builder.append("RPS : ").append(rps).append(" ");
        builder.append("top -> ");
        for (Map.Entry<String, Long> host : top.entrySet()) {
            builder.append(host.getKey()).append(" : ").append(host.getValue()).append("; ");
        }

        log.info(builder.toString());
    }
}
