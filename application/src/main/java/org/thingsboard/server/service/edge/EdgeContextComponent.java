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
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.translation.CustomTranslationService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.processor.AdminSettingsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityGroupEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.GroupPermissionsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RoleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.SchedulerEventEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.TelemetryEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WhiteLabelingEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

@Component
@TbCoreComponent
@Data
public class EdgeContextComponent {

    @Lazy
    @Autowired
    private EdgeService edgeService;

    @Lazy
    @Autowired
    private EdgeEventService edgeEventService;

    @Lazy
    @Autowired
    private AdminSettingsService adminSettingsService;

    @Lazy
    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private DeviceProfileService deviceProfileService;

    @Lazy
    @Autowired
    private AttributesService attributesService;

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
    private ActorService actorService;

    @Lazy
    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Lazy
    @Autowired
    private EdgeRequestsService edgeRequestsService;

    @Lazy
    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Lazy
    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Lazy
    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Lazy
    @Autowired
    private EntityEdgeProcessor entityProcessor;

    @Lazy
    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Lazy
    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Lazy
    @Autowired
    private UserEdgeProcessor userProcessor;

    @Lazy
    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Lazy
    @Autowired
    private TelemetryEdgeProcessor telemetryProcessor;

    @Lazy
    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Lazy
    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Lazy
    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    @Lazy
    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Lazy
    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Lazy
    @Autowired
    private AdminSettingsEdgeProcessor adminSettingsProcessor;

    @Lazy
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    // PE context

    @Lazy
    @Autowired
    protected RoleService roleService;

    @Lazy
    @Autowired
    protected SchedulerEventService schedulerEventService;

    @Lazy
    @Autowired
    protected WhiteLabelingService whiteLabelingService;

    @Lazy
    @Autowired
    protected CustomTranslationService customTranslationService;

    @Lazy
    @Autowired
    protected EntityGroupService entityGroupService;

    @Lazy
    @Autowired
    private EntityGroupEdgeProcessor entityGroupProcessor;

    @Lazy
    @Autowired
    private WhiteLabelingEdgeProcessor whiteLabelingProcessor;

    @Lazy
    @Autowired
    private RoleEdgeProcessor roleProcessor;

    @Lazy
    @Autowired
    private GroupPermissionsEdgeProcessor groupPermissionsProcessor;

    @Lazy
    @Autowired
    private SchedulerEventEdgeProcessor schedulerEventProcessor;
}
