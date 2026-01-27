/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.cloud;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cloud.CloudEventService;
import org.thingsboard.server.dao.cloud.EdgeSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.processor.AdminSettingsCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCommentCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CalculatedFieldCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomerCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DashboardCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EdgeCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.NotificationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.OAuth2CloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.OtaPackageCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.QueueCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ResourceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RuleChainCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.UserCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetBundleCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WidgetTypeCloudProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Lazy
@Data
@Slf4j
@Component
@TbCoreComponent
public class CloudContextComponent {

    private Map<CloudEventType, EdgeProcessor> processorMap = new EnumMap<>(CloudEventType.class);

    @Autowired
    public CloudContextComponent(List<EdgeProcessor> processors) {
        processors.forEach(processor -> {
            CloudEventType eventType = processor.getCloudEventType();
            if (eventType != null) {
                processorMap.put(eventType, processor);
            }
        });
    }

    // services
    @Autowired
    private AttributesService attributesService;

    @Autowired
    private CloudEventService cloudEventService;

    // processors
    @Autowired
    private AdminSettingsCloudProcessor adminSettingsProcessor;

    @Autowired
    private AlarmCommentCloudProcessor alarmCommentProcessor;

    @Autowired
    private AlarmCloudProcessor alarmProcessor;

    @Autowired
    private AssetCloudProcessor assetProcessor;

    @Autowired
    private AssetProfileCloudProcessor assetProfileProcessor;

    @Autowired
    private CustomerCloudProcessor customerProcessor;

    @Autowired
    private DashboardCloudProcessor dashboardProcessor;

    @Autowired
    private DeviceCloudProcessor deviceProcessor;

    @Autowired
    private DeviceProfileCloudProcessor deviceProfileProcessor;

    @Autowired
    private EdgeCloudProcessor edgeProcessor;

    @Autowired
    private EntityViewCloudProcessor entityViewProcessor;

    @Autowired
    private NotificationCloudProcessor notificationProcessor;

    @Autowired
    private OAuth2CloudProcessor oAuth2Processor;

    @Autowired
    private OtaPackageCloudProcessor otaPackageProcessor;

    @Autowired
    private QueueCloudProcessor queueProcessor;

    @Autowired
    private RelationCloudProcessor relationProcessor;

    @Autowired
    private ResourceCloudProcessor resourceProcessor;

    @Autowired
    private RuleChainCloudProcessor ruleChainProcessor;

    @Autowired
    private TelemetryCloudProcessor telemetryProcessor;

    @Autowired
    private TenantCloudProcessor tenantProcessor;

    @Autowired
    private TenantProfileCloudProcessor tenantProfileProcessor;

    @Autowired
    private UserCloudProcessor userProcessor;

    @Autowired
    private WidgetBundleCloudProcessor widgetsBundleProcessor;

    @Autowired
    private WidgetTypeCloudProcessor widgetTypeProcessor;

    // config
    @Autowired
    private EdgeSettingsService edgeSettingsService;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

    @Autowired
    private CalculatedFieldCloudProcessor calculatedFieldProcessor;

    // callback
    @Autowired
    private DbCallbackExecutorService dbCallbackExecutorService;

    public EdgeProcessor getProcessor(CloudEventType cloudEventType) {
        EdgeProcessor processor = processorMap.get(cloudEventType);
        if (processor == null) {
            throw new UnsupportedOperationException("No processor found for CloudEventType: " + cloudEventType);
        }
        return processor;
    }

}
