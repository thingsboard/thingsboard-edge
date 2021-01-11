/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.context.ApplicationListener;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.discovery.PartitionChangeEvent;

/**
 * Created by ashvayka on 02.12.17.
 */
public interface PlatformIntegrationService extends ApplicationListener<PartitionChangeEvent> {

    void validateIntegrationConfiguration(Integration integration);

    void checkIntegrationConnection(Integration integration) throws Exception;

    ListenableFuture<ThingsboardPlatformIntegration> createIntegration(Integration integration);

    ListenableFuture<ThingsboardPlatformIntegration> updateIntegration(Integration integration);

    ListenableFuture<Void> deleteIntegration(IntegrationId integration);

    ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String key);

    void onQueueMsg(TransportProtos.IntegrationDownlinkMsgProto msg, TbCallback callback);

    void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, IntegrationCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, IntegrationCallback<Void> callback);

    void process(TenantId asset, TbMsg tbMsg, IntegrationCallback<Void> callback);

    Device getOrCreateDevice(Integration integration, String deviceName, String deviceType, String customerName, String groupName);

    Asset getOrCreateAsset(Integration configuration, String assetName, String assetType, String customerName, String groupName);

    EntityView getOrCreateEntityView(Integration configuration, Device device, org.thingsboard.server.gen.integration.EntityViewDataProto proto);
}
