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
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.secret.SecretInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class SecretEdgeTest extends AbstractEdgeTest {

    private static final String DEFAULT_SECRET_NAME = "Edge Test Secret";
    private static final String UPDATED_SECRET_DESCRIPTION = "Updated Edge Test Secret";

    @Test
    public void testSecret_create_update_delete() throws Exception {
        // create
        Secret secret = createSecret();

        edgeImitator.expectMessageAmount(1);
        SecretInfo savedSecret = doPost("/api/secret", secret, SecretInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof SecretUpdateMsg);
        SecretUpdateMsg msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, msg.getMsgType());

        Secret secretFromMsg = JacksonUtil.fromString(msg.getEntity(), Secret.class, true);
        Assert.assertEquals(DEFAULT_SECRET_NAME, secretFromMsg.getName());

        // update
        edgeImitator.expectMessageAmount(1);
        savedSecret.setDescription(UPDATED_SECRET_DESCRIPTION);
        secret = new Secret(savedSecret, secret.getRawValue());
        savedSecret = doPost("/api/secret", secret, SecretInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, msg.getMsgType());

        secretFromMsg = JacksonUtil.fromString(msg.getEntity(), Secret.class, true);
        Assert.assertEquals(UPDATED_SECRET_DESCRIPTION, secretFromMsg.getDescription());

        // delete
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/secret/" + savedSecret.getUuidId()).andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        msg = (SecretUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, msg.getMsgType());
    }

    private Secret createSecret() {
        Secret secret = new Secret();
        secret.setTenantId(tenantId);
        secret.setName(SecretEdgeTest.DEFAULT_SECRET_NAME);
        secret.setType(SecretType.TEXT);
        secret.setValue("test-value");
        secret.setDescription("test-description");
        return secret;
    }

}
