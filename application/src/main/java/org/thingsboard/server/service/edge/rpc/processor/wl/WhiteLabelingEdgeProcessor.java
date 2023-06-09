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
package org.thingsboard.server.service.edge.rpc.processor.wl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.gen.edge.v1.CustomTranslationProto;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.LoginWhiteLabelingParamsProto;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingParamsProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@TbCoreComponent
public class WhiteLabelingEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertWhiteLabelingEventToDownlink(EdgeEvent edgeEvent) {
        DownlinkMsg result = null;
        try {
            EntityId entityId = JacksonUtil.OBJECT_MAPPER.convertValue(edgeEvent.getBody(), EntityId.class);
            switch (entityId.getEntityType()) {
                case TENANT:
                    if (EntityId.NULL_UUID.equals(entityId.getId())) {
                        WhiteLabelingParams systemWhiteLabelingParams =
                                whiteLabelingService.getSystemWhiteLabelingParams(edgeEvent.getTenantId());
                        if (isDefaultWhiteLabeling(systemWhiteLabelingParams)) {
                            return null;
                        }
                        WhiteLabelingParamsProto whiteLabelingParamsProto =
                                whiteLabelingParamsProtoConstructor.constructWhiteLabelingParamsProto(systemWhiteLabelingParams, entityId);
                        result = DownlinkMsg.newBuilder()
                                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                .setSystemWhiteLabelingParams(whiteLabelingParamsProto)
                                .build();
                    } else {
                        WhiteLabelingParams tenantWhiteLabelingParams =
                                whiteLabelingService.getTenantWhiteLabelingParams(edgeEvent.getTenantId()).get();
                        if (isDefaultWhiteLabeling(tenantWhiteLabelingParams)) {
                            return null;
                        }
                        WhiteLabelingParamsProto whiteLabelingParamsProto =
                                whiteLabelingParamsProtoConstructor.constructWhiteLabelingParamsProto(tenantWhiteLabelingParams, entityId);
                        result = DownlinkMsg.newBuilder()
                                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                .setTenantWhiteLabelingParams(whiteLabelingParamsProto)
                                .build();
                    }
                    break;
                case CUSTOMER:
                    CustomerId customerId = new CustomerId(entityId.getId());
                    WhiteLabelingParams customerWhiteLabelingParams =
                            whiteLabelingService.getCustomerWhiteLabelingParams(edgeEvent.getTenantId(), customerId).get();
                    if (isDefaultWhiteLabeling(customerWhiteLabelingParams)) {
                        return null;
                    }
                    WhiteLabelingParamsProto whiteLabelingParamsProto =
                            whiteLabelingParamsProtoConstructor.constructWhiteLabelingParamsProto(customerWhiteLabelingParams, customerId);
                    result = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setCustomerWhiteLabelingParams(whiteLabelingParamsProto)
                            .build();
            }
        } catch (Exception e) {
            log.error("Can't process white labeling msg [{}]", edgeEvent, e);
        }
        return result;
    }

    private boolean isDefaultWhiteLabeling(WhiteLabelingParams whiteLabelingParams) {
        return new WhiteLabelingParams().equals(whiteLabelingParams);
    }

    public DownlinkMsg convertLoginWhiteLabelingEventToDownlink(EdgeEvent edgeEvent) {
        DownlinkMsg result = null;
        try {
            EntityId entityId = JacksonUtil.OBJECT_MAPPER.convertValue(edgeEvent.getBody(), EntityId.class);
            switch (entityId.getEntityType()) {
                case TENANT:
                    if (EntityId.NULL_UUID.equals(entityId.getId())) {
                        LoginWhiteLabelingParams systemLoginWhiteLabelingParams =
                                whiteLabelingService.getSystemLoginWhiteLabelingParams(edgeEvent.getTenantId());
                        if (isDefaultLoginWhiteLabeling(systemLoginWhiteLabelingParams)) {
                            return null;
                        }
                        LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                                whiteLabelingParamsProtoConstructor.constructLoginWhiteLabelingParamsProto(systemLoginWhiteLabelingParams, entityId);
                        result = DownlinkMsg.newBuilder()
                                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                .setSystemLoginWhiteLabelingParams(loginWhiteLabelingParamsProto)
                                .build();
                    } else {
                        LoginWhiteLabelingParams tenantLoginWhiteLabelingParams =
                                whiteLabelingService.getTenantLoginWhiteLabelingParams(edgeEvent.getTenantId());
                        if (isDefaultLoginWhiteLabeling(tenantLoginWhiteLabelingParams)) {
                            return null;
                        }
                        LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                                whiteLabelingParamsProtoConstructor.constructLoginWhiteLabelingParamsProto(tenantLoginWhiteLabelingParams, entityId);
                        result = DownlinkMsg.newBuilder()
                                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                .setTenantLoginWhiteLabelingParams(loginWhiteLabelingParamsProto)
                                .build();
                    }
                    break;
                case CUSTOMER:
                    CustomerId customerId = new CustomerId(entityId.getId());
                    LoginWhiteLabelingParams customerLoginWhiteLabelingParams =
                            whiteLabelingService.getCustomerLoginWhiteLabelingParams(edgeEvent.getTenantId(), customerId);
                    if (isDefaultLoginWhiteLabeling(customerLoginWhiteLabelingParams)) {
                        return null;
                    }
                    LoginWhiteLabelingParamsProto loginWhiteLabelingParamsProto =
                            whiteLabelingParamsProtoConstructor.constructLoginWhiteLabelingParamsProto(customerLoginWhiteLabelingParams, customerId);
                    result = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setCustomerLoginWhiteLabelingParams(loginWhiteLabelingParamsProto)
                            .build();
            }
        } catch (Exception e) {
            log.error("Can't process login white labeling msg [{}]", edgeEvent, e);
        }
        return result;
    }

    private boolean isDefaultLoginWhiteLabeling(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        return new LoginWhiteLabelingParams().equals(loginWhiteLabelingParams);
    }

    public DownlinkMsg convertCustomTranslationEventToDownlink(EdgeEvent edgeEvent) {
        DownlinkMsg result = null;
        try {
            EntityId entityId = JacksonUtil.OBJECT_MAPPER.convertValue(edgeEvent.getBody(), EntityId.class);
            switch (entityId.getEntityType()) {
                case TENANT:
                    if (EntityId.NULL_UUID.equals(entityId.getId())) {
                        CustomTranslation systemCustomTranslation =
                                customTranslationService.getSystemCustomTranslation(edgeEvent.getTenantId());
                        if (isDefaultCustomTranslation(systemCustomTranslation)) {
                            return null;
                        }
                        CustomTranslationProto customTranslationProto =
                                customTranslationProtoConstructor.constructCustomTranslationProto(systemCustomTranslation, entityId);
                        result = DownlinkMsg.newBuilder()
                                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                .setSystemCustomTranslationMsg(customTranslationProto)
                                .build();
                    } else {
                        CustomTranslation tenantCustomTranslation =
                                customTranslationService.getTenantCustomTranslation(edgeEvent.getTenantId());
                        if (isDefaultCustomTranslation(tenantCustomTranslation)) {
                            return null;
                        }
                        CustomTranslationProto customTranslationProto =
                                customTranslationProtoConstructor.constructCustomTranslationProto(tenantCustomTranslation, entityId);
                        result = DownlinkMsg.newBuilder()
                                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                .setTenantCustomTranslationMsg(customTranslationProto)
                                .build();
                    }
                    break;
                case CUSTOMER:
                    CustomerId customerId = new CustomerId(entityId.getId());
                    CustomTranslation customerCustomTranslation =
                            customTranslationService.getCustomerCustomTranslation(edgeEvent.getTenantId(), customerId);
                    if (isDefaultCustomTranslation(customerCustomTranslation)) {
                        return null;
                    }
                    CustomTranslationProto customTranslationProto =
                            customTranslationProtoConstructor.constructCustomTranslationProto(customerCustomTranslation, customerId);
                    result = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setCustomerCustomTranslationMsg(customTranslationProto)
                            .build();
            }
        } catch (Exception e) {
            log.error("Can't process custom translation msg [{}]", edgeEvent, e);
        }
        return result;
    }

    private boolean isDefaultCustomTranslation(CustomTranslation customTranslation) {
        return new CustomTranslation().equals(customTranslation);
    }

    public ListenableFuture<Void> processNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(EdgeEventType.valueOf(edgeNotificationMsg.getEntityType()),
                new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        switch (entityId.getEntityType()) {
            case TENANT:
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                    PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                    PageData<TenantId> tenantsIds;
                    do {
                        tenantsIds = tenantService.findTenantsIds(pageLink);
                        for (TenantId tenantId1 : tenantsIds.getData()) {
                            futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, null, JacksonUtil.valueToTree(entityId)));
                        }
                        pageLink = pageLink.nextPageLink();
                    } while (tenantsIds.hasNext());
                } else {
                    futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, null, JacksonUtil.valueToTree(entityId));
                }
                return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
            case CUSTOMER:
                if (EdgeEventActionType.UPDATED.equals(actionType)) {
                    List<EdgeId> edgesByCustomerId =
                            customersHierarchyEdgeService.findAllEdgesInHierarchyByCustomerId(tenantId, new CustomerId(entityId.getId()));
                    if (edgesByCustomerId != null) {
                        for (EdgeId edgeId : edgesByCustomerId) {
                            saveEdgeEvent(tenantId, edgeId, type, actionType, null, JacksonUtil.valueToTree(entityId));
                        }
                    }
                }
                break;
        }
        return Futures.immediateFuture(null);
    }
}
