// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge.stats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;

@ConditionalOnProperty(prefix = "cloud.stats", name = "enabled", havingValue = "true", matchIfMissing = false)
@Service
@Slf4j
public class CloudStatsCounterService {

    private MsgCounters counter;

    public void recordEvent(CloudStatsKey type, TenantId tenantId, long value) {
        initCounter(tenantId);
        switch (type) {
            case UPLINK_MSGS_ADDED -> counter.getMsgsAdded().addAndGet(value);
            case UPLINK_MSGS_PUSHED -> counter.getMsgsPushed().addAndGet(value);
            case UPLINK_MSGS_PERMANENTLY_FAILED -> counter.getMsgsPermanentlyFailed().addAndGet(value);
            case UPLINK_MSGS_TMP_FAILED -> counter.getMsgsTmpFailed().addAndGet(value);
        }
    }

    public void setUplinkMsgsLag(TenantId tenantId, long value) {
        initCounter(tenantId);
        counter.getMsgsLag().set(value);
    }

    public void clear() {
        counter.clear();
    }

    private void initCounter(TenantId tenantId) {
        if (counter == null) {
            counter = new MsgCounters(tenantId);
        }
    }

    public MsgCounters getCounter(TenantId tenantId) {
        initCounter(tenantId);
        return counter;
    }

}
