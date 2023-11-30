/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.edge.rpc.CustomersHierarchyEdgeService;
import org.thingsboard.server.service.edge.rpc.constructor.wl.WhiteLabelingParamsProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.asset.AssetMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.converter.ConverterMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.edge.EdgeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.group.GroupMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.integration.IntegrationMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.queue.QueueMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.queue.QueueMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.queue.QueueMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.role.RoleMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.scheduler.SchedulerEventMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.telemetry.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.user.UserMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.user.UserMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.user.UserMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorFactory;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorV1;
import org.thingsboard.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorV2;
import org.thingsboard.server.service.edge.rpc.constructor.translation.CustomTranslationConstructorFactory;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.UUID;
import java.util.stream.Stream;

public abstract class BaseEdgeProcessorTest {

    @MockBean
    protected TelemetrySubscriptionService tsSubService;

    @MockBean
    protected TbNotificationEntityService notificationEntityService;

    @MockBean
    protected RuleChainService ruleChainService;

    @MockBean
    protected AlarmService alarmService;

    @MockBean
    protected DeviceService deviceService;

    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;

    @MockBean
    protected TbAssetProfileCache assetProfileCache;

    @MockBean
    protected DashboardService dashboardService;

    @MockBean
    protected AssetService assetService;

    @MockBean
    protected EntityViewService entityViewService;

    @MockBean
    protected TenantService tenantService;

    @MockBean
    protected TenantProfileService tenantProfileService;

    @MockBean
    protected EdgeService edgeService;

    @MockBean
    protected CustomerService customerService;

    @MockBean
    protected UserService userService;

    @MockBean
    protected DeviceProfileService deviceProfileService;

    @MockBean
    protected AssetProfileService assetProfileService;

    @MockBean
    protected RelationService relationService;

    @MockBean
    protected DeviceCredentialsService deviceCredentialsService;

    @MockBean
    protected AttributesService attributesService;

    @MockBean
    protected TbClusterService tbClusterService;

    @MockBean
    protected DeviceStateService deviceStateService;

    @MockBean
    protected EdgeEventService edgeEventService;

    @MockBean
    protected WidgetsBundleService widgetsBundleService;

    @MockBean
    protected WidgetTypeService widgetTypeService;

    @MockBean
    protected OtaPackageService otaPackageService;

    @MockBean
    protected QueueService queueService;

    @MockBean
    protected PartitionService partitionService;

    @MockBean
    protected ResourceService resourceService;

    @MockBean
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    @MockBean
    protected DataValidator<Device> deviceValidator;

    @MockBean
    protected DataValidator<DeviceProfile> deviceProfileValidator;

    @MockBean
    protected DataValidator<Asset> assetValidator;

    @MockBean
    protected DataValidator<AssetProfile> assetProfileValidator;

    @MockBean
    protected DataValidator<Dashboard> dashboardValidator;

    @MockBean
    protected DataValidator<EntityView> entityViewValidator;

    @MockBean
    protected DataValidator<TbResource> resourceValidator;

    @MockBean
    protected EdgeMsgConstructor edgeMsgConstructor;

    @MockBean
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    @MockBean
    protected AdminSettingsMsgConstructorV1 adminSettingsMsgConstructorV1;

    @MockBean
    protected AdminSettingsMsgConstructorV2 adminSettingsMsgConstructorV2;

    @MockBean
    protected AlarmMsgConstructorV1 alarmMsgConstructorV1;

    @MockBean
    protected AlarmMsgConstructorV2 alarmMsgConstructorV2;

    @SpyBean
    protected AssetMsgConstructorV1 assetMsgConstructorV1;

    @SpyBean
    protected AssetMsgConstructorV2 assetMsgConstructorV2;

    @MockBean
    protected CustomerMsgConstructorV1 customerMsgConstructorV1;

    @MockBean
    protected CustomerMsgConstructorV2 customerMsgConstructorV2;

    @MockBean
    protected DashboardMsgConstructorV1 dashboardMsgConstructorV1;

    @MockBean
    protected DashboardMsgConstructorV2 dashboardMsgConstructorV2;

    @SpyBean
    protected DeviceMsgConstructorV1 deviceMsgConstructorV1;

    @SpyBean
    protected DeviceMsgConstructorV2 deviceMsgConstructorV2;

    @MockBean
    protected EntityViewMsgConstructorV1 entityViewMsgConstructorV1;

    @MockBean
    protected EntityViewMsgConstructorV2 entityViewMsgConstructorV2;

    @MockBean
    protected OtaPackageMsgConstructorV1 otaPackageMsgConstructorV1;

    @MockBean
    protected OtaPackageMsgConstructorV2 otaPackageMsgConstructorV2;

    @MockBean
    protected QueueMsgConstructorV1 queueMsgConstructorV1;

    @MockBean
    protected QueueMsgConstructorV2 queueMsgConstructorV2;

    @MockBean
    protected RelationMsgConstructorV1 relationMsgConstructorV1;

    @MockBean
    protected RelationMsgConstructorV2 relationMsgConstructorV2;

    @MockBean
    protected ResourceMsgConstructorV1 resourceMsgConstructorV1;

    @MockBean
    protected ResourceMsgConstructorV2 resourceMsgConstructorV2;

    @SpyBean
    protected RuleChainMsgConstructorV1 ruleChainMsgConstructorV1;

    @SpyBean
    protected RuleChainMsgConstructorV2 ruleChainMsgConstructorV2;

    @MockBean
    protected TenantMsgConstructorV1 tenantMsgConstructorV1;

    @MockBean
    protected TenantMsgConstructorV2 tenantMsgConstructorV2;

    @MockBean
    protected UserMsgConstructorV1 userMsgConstructorV1;

    @MockBean
    protected UserMsgConstructorV2 userMsgConstructorV2;

    @MockBean
    protected WidgetMsgConstructorV1 widgetMsgConstructorV1;

    @MockBean
    protected WidgetMsgConstructorV2 widgetMsgConstructorV2;

    @SpyBean
    protected RuleChainMsgConstructorFactory ruleChainMsgConstructorFactory;

    @MockBean
    protected AlarmMsgConstructorFactory alarmMsgConstructorFactory;

    @SpyBean
    protected DeviceMsgConstructorFactory deviceMsgConstructorFactory;

    @SpyBean
    protected AssetMsgConstructorFactory assetMsgConstructorFactory;

    @MockBean
    protected ConverterMsgConstructorFactory converterMsgConstructorFactory;

    @MockBean
    protected DashboardMsgConstructorFactory dashboardMsgConstructorFactory;

    @MockBean
    protected EntityViewMsgConstructorFactory entityViewMsgConstructorFactory;

    @MockBean
    protected GroupMsgConstructorFactory entityGroupMsgConstructorFactory;

    @MockBean
    protected RoleMsgConstructorFactory roleMsgConstructorFactory;

    @MockBean
    protected RelationMsgConstructorFactory relationMsgConstructorFactory;

    @MockBean
    protected UserMsgConstructorFactory userMsgConstructorFactory;

    @MockBean
    protected CustomerMsgConstructorFactory customerMsgConstructorFactory;

    @MockBean
    protected TenantMsgConstructorFactory tenantMsgConstructorFactory;

    @MockBean
    protected WidgetMsgConstructorFactory widgetBundleMsgConstructorFactory;

    @MockBean
    protected AdminSettingsMsgConstructorFactory adminSettingsMsgConstructorFactory;

    @MockBean
    protected OtaPackageMsgConstructorFactory otaPackageMsgConstructorFactory;

    @MockBean
    protected QueueMsgConstructorFactory queueMsgConstructorFactory;

    @MockBean
    protected ResourceMsgConstructorFactory resourceMsgConstructorFactory;

    @MockBean
    protected SchedulerEventMsgConstructorFactory schedulerEventMsgConstructorFactory;

    @MockBean
    protected IntegrationMsgConstructorFactory integrationMsgConstructorFactory;

    @MockBean
    protected CustomTranslationConstructorFactory whiteLabelingConstructorFactory;

    @MockBean
    protected EdgeSynchronizationManager edgeSynchronizationManager;

    @MockBean
    protected DbCallbackExecutorService dbCallbackExecutorService;
    
    @MockBean
    protected DataDecodingEncodingService dataDecodingEncodingService;

    @MockBean
    protected WhiteLabelingService whiteLabelingService;

    @MockBean
    protected CustomTranslationService customTranslationService;

    @MockBean
    protected EntityGroupService entityGroupService;

    @MockBean
    protected RoleService roleService;

    @MockBean
    protected GroupPermissionService groupPermissionService;

    @MockBean
    protected UserPermissionsService userPermissionsService;

    @MockBean
    protected SchedulerEventService schedulerEventService;

    @MockBean
    protected IntegrationService integrationService;

    @MockBean
    protected ConverterService converterService;

    @MockBean
    protected WhiteLabelingParamsProtoConstructor whiteLabelingParamsProtoConstructor;


    @MockBean
    protected CustomersHierarchyEdgeService customersHierarchyEdgeService;

    @MockBean
    protected OwnersCacheService ownersCacheService;
    
    protected EdgeId edgeId;
    protected TenantId tenantId;
    protected EdgeEvent edgeEvent;

    protected DashboardId getDashboardId(long expectedDashboardIdMSB, long expectedDashboardIdLSB) {
        DashboardId dashboardId;
        if (expectedDashboardIdMSB != 0 && expectedDashboardIdLSB != 0) {
            dashboardId = new DashboardId(new UUID(expectedDashboardIdMSB, expectedDashboardIdLSB));
        } else {
            dashboardId = new DashboardId(UUID.randomUUID());
        }
        return dashboardId;
    }

    protected RuleChainId getRuleChainId(long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        RuleChainId ruleChainId;
        if (expectedRuleChainIdMSB != 0 && expectedRuleChainIdLSB != 0) {
            ruleChainId = new RuleChainId(new UUID(expectedRuleChainIdMSB, expectedRuleChainIdLSB));
        } else {
            ruleChainId = new RuleChainId(UUID.randomUUID());
        }
        return ruleChainId;
    }

    protected static Stream<Arguments> provideParameters() {
        UUID dashoboardUUID = UUID.randomUUID();
        UUID ruleChaindUUID = UUID.randomUUID();
        return Stream.of(
                Arguments.of(EdgeVersion.V_3_3_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_3_3, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_4_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_6_0,
                        dashoboardUUID.getMostSignificantBits(),
                        dashoboardUUID.getLeastSignificantBits(),
                        ruleChaindUUID.getMostSignificantBits(),
                        ruleChaindUUID.getLeastSignificantBits())
        );
    }
}
