/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.constructor.AlarmUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.SchedulerEventUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.init.InitEdgeService;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainUpdateMsgConstructor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.state.DeviceStateService;

@Component
@Data
public class EdgeContextComponent {

    @Lazy
    @Autowired
    private EdgeService edgeService;

    @Lazy
    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private DeviceService deviceService;

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
    private ActorService actorService;

    @Lazy
    @Autowired
    private DeviceStateService deviceStateService;

    @Lazy
    @Autowired
    private TbClusterService tbClusterService;

    @Lazy
    @Autowired
    private InitEdgeService initEdgeService;

    @Lazy
    @Autowired
    private RuleChainUpdateMsgConstructor ruleChainUpdateMsgConstructor;

    @Lazy
    @Autowired
    private SchedulerEventUpdateMsgConstructor schedulerEventUpdateMsgConstructor;

    @Lazy
    @Autowired
    private AlarmUpdateMsgConstructor alarmUpdateMsgConstructor;

    @Lazy
    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Lazy
    @Autowired
    private AssetUpdateMsgConstructor assetUpdateMsgConstructor;

    @Lazy
    @Autowired
    private EntityViewUpdateMsgConstructor entityViewUpdateMsgConstructor;

    @Lazy
    @Autowired
    private DashboardUpdateMsgConstructor dashboardUpdateMsgConstructor;

    @Lazy
    @Autowired
    private UserUpdateMsgConstructor userUpdateMsgConstructor;

    @Lazy
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;
}
