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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
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
import org.thingsboard.server.service.edge.rpc.constructor.AdminSettingsMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.ConverterProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomTranslationProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomerMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EdgeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityGroupMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.GroupPermissionProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.IntegrationProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.OtaPackageMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.QueueMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RoleProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.SchedulerEventMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.TenantMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.TenantProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WhiteLabelingParamsProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetTypeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetsBundleMsgConstructor;
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
    protected EdgeMsgConstructor edgeMsgConstructor;

    @MockBean
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    @MockBean
    protected RuleChainMsgConstructor ruleChainMsgConstructor;

    @MockBean
    protected AlarmMsgConstructor alarmMsgConstructor;

    @SpyBean
    protected DeviceMsgConstructor deviceMsgConstructor;

    @SpyBean
    protected AssetMsgConstructor assetMsgConstructor;

    @MockBean
    protected EntityViewMsgConstructor entityViewMsgConstructor;

    @MockBean
    protected DashboardMsgConstructor dashboardMsgConstructor;

    @MockBean
    protected RelationMsgConstructor relationMsgConstructor;

    @MockBean
    protected UserMsgConstructor userMsgConstructor;

    @MockBean
    protected CustomerMsgConstructor customerMsgConstructor;

    @SpyBean
    protected DeviceProfileMsgConstructor deviceProfileMsgConstructor;

    @SpyBean
    protected AssetProfileMsgConstructor assetProfileMsgConstructor;

    @MockBean
    protected TenantMsgConstructor tenantMsgConstructor;

    @MockBean
    protected TenantProfileMsgConstructor tenantProfileMsgConstructor;

    @MockBean
    protected WidgetsBundleMsgConstructor widgetsBundleMsgConstructor;

    @MockBean
    protected WidgetTypeMsgConstructor widgetTypeMsgConstructor;

    @MockBean
    protected AdminSettingsMsgConstructor adminSettingsMsgConstructor;

    @MockBean
    protected OtaPackageMsgConstructor otaPackageMsgConstructor;

    @MockBean
    protected QueueMsgConstructor queueMsgConstructor;

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
    protected CustomTranslationProtoConstructor customTranslationProtoConstructor;

    @MockBean
    protected RoleProtoConstructor roleProtoConstructor;

    @MockBean
    protected GroupPermissionProtoConstructor groupPermissionProtoConstructor;

    @MockBean
    protected SchedulerEventMsgConstructor schedulerEventMsgConstructor;

    @MockBean
    protected EntityGroupMsgConstructor entityGroupMsgConstructor;

    @MockBean
    protected ConverterProtoConstructor converterProtoConstructor;

    @MockBean
    protected IntegrationProtoConstructor integrationProtoConstructor;

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
