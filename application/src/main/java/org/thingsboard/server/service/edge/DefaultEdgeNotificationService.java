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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.asset.AssetProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.converter.ConverterEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.device.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.group.EntityGroupEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.group.GroupPermissionsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.integration.IntegrationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.role.RoleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.scheduler.SchedulerEventEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.wl.WhiteLabelingEdgeProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    public static final String EDGE_IS_ROOT_BODY_KEY = "isRoot";

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EdgeProcessor edgeProcessor;

    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Autowired
    private UserEdgeProcessor userProcessor;

    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Autowired
    private AssetProfileEdgeProcessor assetProfileProcessor;

    @Autowired
    private OtaPackageEdgeProcessor otaPackageProcessor;

    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Autowired
    private QueueEdgeProcessor queueProcessor;

    @Autowired
    private TenantEdgeProcessor tenantEdgeProcessor;

    @Autowired
    private TenantProfileEdgeProcessor tenantProfileEdgeProcessor;

    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    private ExecutorService dbCallBackExecutor;

    // PE context

    @Autowired
    private RoleEdgeProcessor roleProcessor;

    @Autowired
    private GroupPermissionsEdgeProcessor groupPermissionsProcessor;

    @Autowired
    private SchedulerEventEdgeProcessor schedulerEventProcessor;

    @Autowired
    private EntityGroupEdgeProcessor entityGroupProcessor;

    @Autowired
    private IntegrationEdgeProcessor integrationProcessor;

    @Autowired
    private ConverterEdgeProcessor converterProcessor;

    @Autowired
    private WhiteLabelingEdgeProcessor whiteLabelingProcessor;

    @PostConstruct
    public void initExecutor() {
        dbCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edge-notifications"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (dbCallBackExecutor != null) {
            dbCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        ObjectNode isRootBody = JacksonUtil.newObjectNode();
        isRootBody.put(EDGE_IS_ROOT_BODY_KEY, Boolean.TRUE);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edge.getId()).entityId(ruleChainId)
                .body(JacksonUtil.toString(isRootBody)).actionType(ActionType.UPDATED).build());
        return savedEdge;
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(edgeNotificationMsg.getTenantIdMSB(), edgeNotificationMsg.getTenantIdLSB()));
        log.debug("[{}] Pushing notification to edge {}", tenantId, edgeNotificationMsg);
        try {
            EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
            ListenableFuture<Void> future;
            switch (type) {
                case EDGE:
                    future = edgeProcessor.processEdgeNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET:
                    future = assetProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE:
                    future = deviceProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ENTITY_VIEW:
                    future = entityViewProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case DASHBOARD:
                    future = dashboardProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case RULE_CHAIN:
                    future = ruleChainProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case USER:
                    future = userProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case CUSTOMER:
                    future = customerProcessor.processCustomerNotification(tenantId, edgeNotificationMsg);
                    break;
                case DEVICE_PROFILE:
                    future = deviceProfileProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ASSET_PROFILE:
                    future = assetProfileProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case OTA_PACKAGE:
                    future = otaPackageProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGETS_BUNDLE:
                    future = widgetBundleProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WIDGET_TYPE:
                    future = widgetTypeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case QUEUE:
                    future = queueProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ALARM:
                    future = alarmProcessor.processAlarmNotification(tenantId, edgeNotificationMsg);
                    break;
                case RELATION:
                    future = relationProcessor.processRelationNotification(tenantId, edgeNotificationMsg);
                    break;
                case ROLE:
                    future = roleProcessor.processRoleNotification(tenantId, edgeNotificationMsg);
                    break;
                case GROUP_PERMISSION:
                    future = groupPermissionsProcessor.processGroupPermissionNotification(tenantId, edgeNotificationMsg);
                    break;
                case SCHEDULER_EVENT:
                    future = schedulerEventProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case ENTITY_GROUP:
                    future = entityGroupProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case INTEGRATION:
                    future = integrationProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case CONVERTER:
                    future = converterProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case WHITE_LABELING:
                case LOGIN_WHITE_LABELING:
                case CUSTOM_TRANSLATION:
                    future = whiteLabelingProcessor.processNotification(tenantId, edgeNotificationMsg);
                    break;
                case TENANT:
                    future = tenantEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                case TENANT_PROFILE:
                    future = tenantProfileEdgeProcessor.processEntityNotification(tenantId, edgeNotificationMsg);
                    break;
                default:
                    log.warn("[{}] Edge event type [{}] is not designed to be pushed to edge", tenantId, type);
                    future = Futures.immediateFuture(null);
            }
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void unused) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    callBackFailure(tenantId, edgeNotificationMsg, callback, throwable);
                }
            }, dbCallBackExecutor);
        } catch (Exception e) {
            callBackFailure(tenantId, edgeNotificationMsg, callback, e);
        }
    }

    private void callBackFailure(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback, Throwable throwable) {
        log.error("[{}] Can't push to edge updates, edgeNotificationMsg [{}]", tenantId, edgeNotificationMsg, throwable);
        callback.onFailure(throwable);
    }
}
