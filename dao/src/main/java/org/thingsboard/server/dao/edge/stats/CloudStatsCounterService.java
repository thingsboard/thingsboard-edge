/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.edge.stats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;

@ConditionalOnProperty(prefix = "cloud.stats", name = "enabled", havingValue = "true", matchIfMissing = false)
@Service
@Slf4j
public class CloudStatsCounterService {

    private volatile EdgeStats edgeStats;

    public void recordEvent(CloudStatsKey type, TenantId tenantId, long value) {
        initEdgeStats(tenantId);
        EdgeStats currentStats = edgeStats;
        MsgCounters counter = currentStats.getMsgCounters();
        switch (type) {
            case UPLINK_MSGS_ADDED -> counter.getMsgsAdded().addAndGet(value);
            case UPLINK_MSGS_PUSHED -> counter.getMsgsPushed().addAndGet(value);
            case UPLINK_MSGS_PERMANENTLY_FAILED -> counter.getMsgsPermanentlyFailed().addAndGet(value);
            case UPLINK_MSGS_TMP_FAILED -> counter.getMsgsTmpFailed().addAndGet(value);
            case UPLINK_MSGS_LAG -> counter.getMsgsLag().set(value);
            case UPLINK_RATE -> currentStats.getUplinkRate().add(value);
        }
    }

    private void initEdgeStats(TenantId tenantId) {
        if (edgeStats == null) {
            synchronized (this) {
                if (edgeStats == null) {
                    edgeStats = new EdgeStats(tenantId);
                }
            }
        }
    }

    public void resetStats() {
        synchronized (this) {
            if (edgeStats != null) {
                edgeStats = new EdgeStats(edgeStats.getMsgCounters().getTenantId());
            }
        }
    }

    public EdgeStats getEdgeStats(TenantId tenantId) {
        initEdgeStats(tenantId);
        return edgeStats;
    }

}
