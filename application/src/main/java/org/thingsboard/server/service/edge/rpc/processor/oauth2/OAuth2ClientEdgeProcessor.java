/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.edge.rpc.processor.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class OAuth2ClientEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        OAuth2ClientId oAuth2ClientId = new OAuth2ClientId(edgeEvent.getEntityId());

        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                boolean isPropagateToEdge = edgeCtx.getOAuth2ClientService().isPropagateOAuth2ClientToEdge(edgeEvent.getTenantId(), oAuth2ClientId);
                if (!isPropagateToEdge) {
                    return null;
                }
                OAuth2Client oAuth2Client = edgeCtx.getOAuth2ClientService().findOAuth2ClientById(edgeEvent.getTenantId(), oAuth2ClientId);
                if (oAuth2Client != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg = EdgeMsgConstructorUtils.constructOAuth2ClientUpdateMsg(msgType, oAuth2Client);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addOAuth2ClientUpdateMsg(oAuth2ClientUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                OAuth2ClientUpdateMsg oAuth2ClientDeleteMsg = EdgeMsgConstructorUtils.constructOAuth2ClientDeleteMsg(oAuth2ClientId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addOAuth2ClientUpdateMsg(oAuth2ClientDeleteMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.OAUTH2_CLIENT;
    }

}
