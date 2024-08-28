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
package org.thingsboard.server.service.edge.rpc.constructor.oauth2;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class OAuth2MsgConstructor {

    public OAuth2ClientUpdateMsg constructOAuth2ClientUpdateMsg(UpdateMsgType msgType, OAuth2Client oAuth2Client) {
        return OAuth2ClientUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(oAuth2Client))
                .setIdMSB(oAuth2Client.getId().getId().getMostSignificantBits())
                .setIdLSB(oAuth2Client.getId().getId().getLeastSignificantBits()).build();
    }

    public OAuth2ClientUpdateMsg constructOAuth2ClientDeleteMsg(OAuth2ClientId oAuth2ClientId) {
        return OAuth2ClientUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(oAuth2ClientId.getId().getMostSignificantBits())
                .setIdLSB(oAuth2ClientId.getId().getLeastSignificantBits()).build();
    }

    public OAuth2DomainUpdateMsg constructOAuth2DomainUpdateMsg(UpdateMsgType msgType, DomainInfo domainInfo) {
        return OAuth2DomainUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(domainInfo))
                .setIdMSB(domainInfo.getId().getId().getMostSignificantBits())
                .setIdLSB(domainInfo.getId().getId().getLeastSignificantBits()).build();
    }

    public OAuth2DomainUpdateMsg constructOAuth2DomainDeleteMsg(DomainId domainId) {
        return OAuth2DomainUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(domainId.getId().getMostSignificantBits())
                .setIdLSB(domainId.getId().getLeastSignificantBits())
                .build();
    }

}
