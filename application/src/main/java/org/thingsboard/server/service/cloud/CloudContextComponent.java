/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.cloud.rpc.CloudEventStorageSettings;
import org.thingsboard.server.service.cloud.rpc.processor.AdminSettingsCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AlarmCommentCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.AssetProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ConverterCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomMenuCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomTranslationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.CustomerCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DashboardCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.DeviceProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EdgeCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityGroupCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.EntityViewCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.GroupPermissionCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.IntegrationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.NotificationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.OAuth2CloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.OtaPackageCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.QueueCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RelationCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.ResourceCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RoleCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.RuleChainCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.SchedulerEventCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TelemetryCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.TenantProfileCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.UserCloudProcessor;
import org.thingsboard.server.service.cloud.rpc.processor.WhiteLabelingCloudProcessor;
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

    @Autowired
    private WhiteLabelingService whiteLabelingService;

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

    // PE processors
    @Autowired
    private ConverterCloudProcessor converterProcessor;

    @Autowired
    private CustomMenuCloudProcessor customMenuProcessor;

    @Autowired
    private CustomTranslationCloudProcessor customTranslationProcessor;

    @Autowired
    private EntityGroupCloudProcessor entityGroupProcessor;

    @Autowired
    private GroupPermissionCloudProcessor groupPermissionProcessor;

    @Autowired
    private IntegrationCloudProcessor integrationProcessor;

    @Autowired
    private RoleCloudProcessor roleProcessor;

    @Autowired
    private SchedulerEventCloudProcessor schedulerEventProcessor;

    @Autowired
    private WhiteLabelingCloudProcessor whiteLabelingProcessor;

    // config
    @Autowired
    private EdgeSettingsService edgeSettingsService;

    @Autowired
    private CloudEventStorageSettings cloudEventStorageSettings;

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
