/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.constructor.AdminSettingsMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomTranslationProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomerMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityGroupMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.GroupPermissionProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RoleProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.SchedulerEventMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WhiteLabelingParamsProtoConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetTypeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetsBundleMsgConstructor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.edge.rpc.processor.AlarmProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DeviceProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RelationProcessor;
import org.thingsboard.server.service.edge.rpc.processor.TelemetryProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.state.DeviceStateService;

@Component
@TbCoreComponent
@Data
public class EdgeContextComponent {

    @Lazy
    @Autowired
    private EdgeService edgeService;

    @Autowired
    private PartitionService partitionService;

    @Lazy
    @Autowired
    private EdgeEventService edgeEventService;

    @Lazy
    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private DeviceService deviceService;

    @Lazy
    @Autowired
    private DeviceProfileService deviceProfileService;

    @Lazy
    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Lazy
    @Autowired
    private EntityViewService entityViewService;

    @Lazy
    @Autowired
    private AttributesService attributesService;

    @Lazy
    @Autowired
    private CustomerService customerService;

    @Lazy
    @Autowired
    private RelationService relationService;

    @Lazy
    @Autowired
    private AlarmService alarmService;

    @Lazy
    @Autowired
    private DashboardService dashboardService;

    @Lazy
    @Autowired
    private RuleChainService ruleChainService;

    @Lazy
    @Autowired
    private UserService userService;

    @Lazy
    @Autowired
    private SchedulerEventService schedulerEventService;

    @Lazy
    @Autowired
    private EntityGroupService entityGroupService;

    @Lazy
    @Autowired
    private RoleService roleService;

    @Lazy
    @Autowired
    private GroupPermissionService groupPermissionService;

    @Lazy
    @Autowired
    private UserPermissionsService userPermissionsService;

    @Lazy
    @Autowired
    private ActorService actorService;

    @Lazy
    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Lazy
    @Autowired
    private WidgetTypeService widgetTypeService;

    @Lazy
    @Autowired
    private DeviceStateService deviceStateService;

    @Lazy
    @Autowired
    private TbClusterService tbClusterService;

    @Lazy
    @Autowired
    private EdgeRequestsService edgeRequestsService;

    @Lazy
    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Lazy
    @Autowired
    private CustomTranslationService customTranslationService;

    @Lazy
    @Autowired
    private RuleChainMsgConstructor ruleChainMsgConstructor;

    @Lazy
    @Autowired
    private AlarmMsgConstructor alarmMsgConstructor;

    @Lazy
    @Autowired
    private DeviceMsgConstructor deviceMsgConstructor;

    @Lazy
    @Autowired
    private DeviceProfileMsgConstructor deviceProfileMsgConstructor;

    @Lazy
    @Autowired
    private AssetMsgConstructor assetMsgConstructor;

    @Lazy
    @Autowired
    private EntityViewMsgConstructor entityViewMsgConstructor;

    @Lazy
    @Autowired
    private DashboardMsgConstructor dashboardMsgConstructor;

    @Lazy
    @Autowired
    private CustomerMsgConstructor customerMsgConstructor;

    @Lazy
    @Autowired
    private UserMsgConstructor userMsgConstructor;

    @Lazy
    @Autowired
    private RelationMsgConstructor relationMsgConstructor;

    @Lazy
    @Autowired
    private WidgetsBundleMsgConstructor widgetsBundleMsgConstructor;

    @Lazy
    @Autowired
    private WidgetTypeMsgConstructor widgetTypeMsgConstructor;

    @Lazy
    @Autowired
    private AdminSettingsMsgConstructor adminSettingsMsgConstructor;

    @Lazy
    @Autowired
    private EntityDataMsgConstructor entityDataMsgConstructor;

    @Lazy
    @Autowired
    private SchedulerEventMsgConstructor schedulerEventMsgConstructor;

    @Lazy
    @Autowired
    private EntityGroupMsgConstructor entityGroupMsgConstructor;

    @Lazy
    @Autowired
    private WhiteLabelingParamsProtoConstructor whiteLabelingParamsProtoConstructor;

    @Lazy
    @Autowired
    private CustomTranslationProtoConstructor customTranslationProtoConstructor;

    @Lazy
    @Autowired
    private RoleProtoConstructor roleProtoConstructor;

    @Lazy
    @Autowired
    private GroupPermissionProtoConstructor groupPermissionProtoConstructor;

    @Lazy
    @Autowired
    private AlarmProcessor alarmProcessor;

    @Lazy
    @Autowired
    private DeviceProcessor deviceProcessor;

    @Lazy
    @Autowired
    private RelationProcessor relationProcessor;

    @Lazy
    @Autowired
    private TelemetryProcessor telemetryProcessor;

    @Lazy
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;
}
