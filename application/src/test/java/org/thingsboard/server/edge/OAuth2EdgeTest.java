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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@DaoSqlTest
public class OAuth2EdgeTest extends AbstractEdgeTest {

    @Test
    @Ignore
    public void testOAuth2DomainSupport() throws Exception {
        loginSysAdmin();

        // enable oauth and save domain
        edgeImitator.allowIgnoredTypes();
        edgeImitator.expectMessageAmount(1);
        Domain savedDomain = doPost("/api/domain", constructDomain(), Domain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2DomainUpdateMsg);
        OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = (OAuth2DomainUpdateMsg) latestMessage;
        Domain result = JacksonUtil.fromString(oAuth2DomainUpdateMsg.getEntity(), Domain.class, true);
        Assert.assertEquals(savedDomain, result);

        // disable oauth support: no update of domain events is sending to Edge
        edgeImitator.expectMessageAmount(1);
        savedDomain.setPropagateToEdge(false);
        doPost("/api/domain", savedDomain, Domain.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // delete domain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/domain/" + savedDomain.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2DomainUpdateMsg);
        oAuth2DomainUpdateMsg = (OAuth2DomainUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, oAuth2DomainUpdateMsg.getMsgType());
        Assert.assertEquals(savedDomain.getUuidId().getMostSignificantBits(), oAuth2DomainUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDomain.getUuidId().getLeastSignificantBits(), oAuth2DomainUpdateMsg.getIdLSB());

        edgeImitator.ignoreType(OAuth2DomainUpdateMsg.class);
        edgeImitator.ignoreType(OAuth2ClientUpdateMsg.class);
        loginTenantAdmin();
    }

    @Test
    @Ignore
    public void testOAuth2ClientSupport() throws Exception {
        loginSysAdmin();

        // enable oauth and save domain
        edgeImitator.allowIgnoredTypes();

        edgeImitator.expectMessageAmount(2);
        OAuth2Client savedOAuth2Client = validClientInfo(TenantId.SYS_TENANT_ID, "test edge google client");
        savedOAuth2Client = doPost("/api/oauth2/client", savedOAuth2Client, OAuth2Client.class);
        Domain savedDomain = doPost("/api/domain?oauth2ClientIds=" + savedOAuth2Client.getId().getId(), constructDomain(), Domain.class);

        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<OAuth2DomainUpdateMsg> oAuth2DomainUpdateMsgOpt = edgeImitator.findMessageByType(OAuth2DomainUpdateMsg.class);
        Assert.assertTrue(oAuth2DomainUpdateMsgOpt.isPresent());
        Domain result = JacksonUtil.fromString(oAuth2DomainUpdateMsgOpt.get().getEntity(), Domain.class, true);
        Assert.assertEquals(savedDomain, result);

        Optional<OAuth2ClientUpdateMsg> oAuth2ClientUpdateMsgOpt = edgeImitator.findMessageByType(OAuth2ClientUpdateMsg.class);
        Assert.assertTrue(oAuth2ClientUpdateMsgOpt.isPresent());
        OAuth2Client clientResult = JacksonUtil.fromString(oAuth2ClientUpdateMsgOpt.get().getEntity(), OAuth2Client.class, true);
        Assert.assertEquals(savedOAuth2Client, clientResult);

        // disable oauth support: no update of domain events and client events are sending to Edge
        edgeImitator.expectMessageAmount(1);
        savedDomain.setPropagateToEdge(false);
        doPost("/api/domain", savedDomain, Domain.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        edgeImitator.expectMessageAmount(1);
        savedOAuth2Client.setTitle("Updated title");
        doPost("/api/oauth2/client", savedOAuth2Client, OAuth2Client.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // delete oauth2Client
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/oauth2/client/" + savedOAuth2Client.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2ClientUpdateMsg);
        OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg = (OAuth2ClientUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, oAuth2ClientUpdateMsg.getMsgType());
        Assert.assertEquals(savedOAuth2Client.getUuidId().getMostSignificantBits(), oAuth2ClientUpdateMsg.getIdMSB());
        Assert.assertEquals(savedOAuth2Client.getUuidId().getLeastSignificantBits(), oAuth2ClientUpdateMsg.getIdLSB());

        // delete domain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/domain/" + savedDomain.getId().getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OAuth2DomainUpdateMsg);
        OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = (OAuth2DomainUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, oAuth2DomainUpdateMsg.getMsgType());
        Assert.assertEquals(savedDomain.getUuidId().getMostSignificantBits(), oAuth2DomainUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDomain.getUuidId().getLeastSignificantBits(), oAuth2DomainUpdateMsg.getIdLSB());

        edgeImitator.ignoreType(OAuth2DomainUpdateMsg.class);
        edgeImitator.ignoreType(OAuth2ClientUpdateMsg.class);
        loginTenantAdmin();
    }

    private OAuth2Client validClientInfo(TenantId tenantId, String title) {
        OAuth2Client oAuth2Client = new OAuth2Client();
        oAuth2Client.setTenantId(tenantId);
        oAuth2Client.setTitle(title);
        oAuth2Client.setClientId(UUID.randomUUID().toString());
        oAuth2Client.setClientSecret(UUID.randomUUID().toString());
        oAuth2Client.setAuthorizationUri(UUID.randomUUID().toString());
        oAuth2Client.setAccessTokenUri(UUID.randomUUID().toString());
        oAuth2Client.setScope(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        oAuth2Client.setPlatforms(Collections.emptyList());
        oAuth2Client.setUserInfoUri(UUID.randomUUID().toString());
        oAuth2Client.setUserNameAttributeName(UUID.randomUUID().toString());
        oAuth2Client.setJwkSetUri(UUID.randomUUID().toString());
        oAuth2Client.setClientAuthenticationMethod(UUID.randomUUID().toString());
        oAuth2Client.setLoginButtonLabel(UUID.randomUUID().toString());
        oAuth2Client.setLoginButtonIcon(UUID.randomUUID().toString());
        oAuth2Client.setAdditionalInfo(JacksonUtil.newObjectNode().put(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        oAuth2Client.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .allowUserCreation(true)
                        .activateUser(true)
                        .type(MapperType.CUSTOM)
                        .custom(
                                OAuth2CustomMapperConfig.builder()
                                        .url(UUID.randomUUID().toString())
                                        .build()
                        )
                        .build());
        return oAuth2Client;
    }

    private Domain constructDomain() {
        Domain domain = new Domain();
        domain.setTenantId(TenantId.SYS_TENANT_ID);
        domain.setName("my.edge.domain");
        domain.setOauth2Enabled(true);
        domain.setPropagateToEdge(true);
        return domain;
    }

}
