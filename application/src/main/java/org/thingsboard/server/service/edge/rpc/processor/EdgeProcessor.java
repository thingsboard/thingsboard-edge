/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

public interface EdgeProcessor {

    ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto msg);

    default DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        return null;
    }

    default EdgeEventType getEdgeEventType() {
        return null;
    }

    // Edge-only:
    default UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        return null;
    }

    default CloudEventType getCloudEventType() {
        return null;
    }
    // ... Edge

}
