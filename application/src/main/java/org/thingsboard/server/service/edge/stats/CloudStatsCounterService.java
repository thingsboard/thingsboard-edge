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
package org.thingsboard.server.service.edge.stats;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;

@Getter
@Service
@Slf4j
public class CloudStatsCounterService {

    private MsgCounters uplinkCounters;

    public void recordEvent(CounterEventType type, TenantId tenantId, long value) {
        if (uplinkCounters == null) {
            uplinkCounters = new MsgCounters(tenantId);
        }
        switch (type) {
            case DOWNLINK_MSG_ADDED -> uplinkCounters.getMsgsAdded().addAndGet(value);
            case DOWNLINK_MSG_PUSHED -> uplinkCounters.getMsgsPushed().addAndGet(value);
            case DOWNLINK_MSG_PERMANENTLY_FAILED -> uplinkCounters.getMsgsPermanentlyFailed().addAndGet(value);
            case DOWNLINK_MSG_TMP_FAILED -> uplinkCounters.getMsgsTmpFailed().addAndGet(value);
        }
    }

    public void setUplinkMsgsLag(long value) {
        uplinkCounters.getMsgsLag().set(value);
    }

    public void clear() {
        uplinkCounters.clear();
    }

}
