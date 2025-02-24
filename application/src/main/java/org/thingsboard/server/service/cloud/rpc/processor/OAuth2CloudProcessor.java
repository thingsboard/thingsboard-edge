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
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@TbCoreComponent
public class OAuth2CloudProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processOAuth2ClientMsgFromCloud(OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg) {
        try {
            switch (oAuth2ClientUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    OAuth2Client oAuth2Client = JacksonUtil.fromString(oAuth2ClientUpdateMsg.getEntity(), OAuth2Client.class, true);
                    if (oAuth2Client == null) {
                        throw new RuntimeException("[{" + TenantId.SYS_TENANT_ID + "}] oAuth2ClientUpdateMsg {" + oAuth2ClientUpdateMsg + "} cannot be converted to OAuth2Client");
                    }
                    oAuth2Client.getMapperConfig().setActivateUser(false);
                    oAuth2Client.getMapperConfig().setAllowUserCreation(false);
                    edgeCtx.getOAuth2ClientService().saveOAuth2Client(oAuth2Client.getTenantId(), oAuth2Client);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    OAuth2ClientId oAuth2ClientId = new OAuth2ClientId(new UUID(oAuth2ClientUpdateMsg.getIdMSB(), oAuth2ClientUpdateMsg.getIdLSB()));
                    OAuth2Client oAuth2ClientById = edgeCtx.getOAuth2ClientService().findOAuth2ClientById(TenantId.SYS_TENANT_ID, oAuth2ClientId);
                    if (oAuth2ClientById != null) {
                        edgeCtx.getOAuth2ClientService().deleteOAuth2ClientById(TenantId.SYS_TENANT_ID, oAuth2ClientId);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(oAuth2ClientUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process oAuth2 client update msg %s", oAuth2ClientUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processDomainMsgFromCloud(OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg) {
        try {
            switch (oAuth2DomainUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE: {
                    Domain domain = JacksonUtil.fromString(oAuth2DomainUpdateMsg.getEntity(), Domain.class, true);
                    if (domain == null) {
                        throw new RuntimeException("[{" + TenantId.SYS_TENANT_ID + "}] oAuth2DomainUpdateMsg {" + oAuth2DomainUpdateMsg + "} cannot be converted to Domain");
                    }
                    DomainInfo domainInfo = JacksonUtil.fromString(oAuth2DomainUpdateMsg.getEntity(), DomainInfo.class, true);
                    if (domainInfo == null) {
                        throw new RuntimeException("[{" + TenantId.SYS_TENANT_ID + "}] oAuth2DomainUpdateMsg {" + oAuth2DomainUpdateMsg + "} cannot be converted to DomainInfo");
                    }
                    if (domain.isOauth2Enabled() && !domain.isPropagateToEdge()) {
                        domain.setOauth2Enabled(false);
                    }
                    Domain savedDomain = edgeCtx.getDomainService().saveDomain(TenantId.SYS_TENANT_ID, domain);
                    List<OAuth2ClientId> oAuth2Clients = domainInfo.getOauth2ClientInfos().stream().map(IdBased::getId).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(oAuth2Clients)) {
                        edgeCtx.getDomainService().updateOauth2Clients(savedDomain.getTenantId(), savedDomain.getId(), oAuth2Clients);
                    }
                    return Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE:
                    DomainId domainId = new DomainId(new UUID(oAuth2DomainUpdateMsg.getIdMSB(), oAuth2DomainUpdateMsg.getIdLSB()));
                    Domain domain = edgeCtx.getDomainService().findDomainById(TenantId.SYS_TENANT_ID, domainId);
                    if (domain != null) {
                        edgeCtx.getDomainService().deleteDomainById(TenantId.SYS_TENANT_ID, domainId);
                    }
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(oAuth2DomainUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            String errMsg = String.format("Can't process domain update msg %s", oAuth2DomainUpdateMsg);
            log.error(errMsg, e);
            return Futures.immediateFailedFuture(new RuntimeException(errMsg, e));
        }
        return Futures.immediateFuture(null);
    }

}
