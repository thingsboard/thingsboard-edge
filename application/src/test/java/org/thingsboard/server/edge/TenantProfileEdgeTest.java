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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

@DaoSqlTest
public class TenantProfileEdgeTest extends AbstractEdgeTest {

    @Test
    @Ignore
    public void testTenantProfiles() throws Exception {
        loginSysAdmin();

        // save current values into tmp to revert after test
        TenantProfile edgeTenantProfile = doGet("/api/tenantProfile/" + tenantProfileId.getId(), TenantProfile.class);

        // updated edge tenant profile
        edgeTenantProfile.setName("Tenant Profile Edge Test");
        edgeTenantProfile.setDescription("Updated tenant profile Edge Test");
        edgeImitator.expectMessageAmount(1);
        edgeTenantProfile = doPost("/api/tenantProfile", edgeTenantProfile, TenantProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof TenantProfileUpdateMsg);
        TenantProfileUpdateMsg tenantProfileUpdateMsg = (TenantProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantProfileUpdateMsg.getMsgType());
        Assert.assertEquals(edgeTenantProfile.getUuidId().getMostSignificantBits(), tenantProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(edgeTenantProfile.getUuidId().getLeastSignificantBits(), tenantProfileUpdateMsg.getIdLSB());
        Assert.assertEquals(edgeTenantProfile.getDescription(), tenantProfileUpdateMsg.getDescription());
        Assert.assertEquals("Updated tenant profile Edge Test", tenantProfileUpdateMsg.getDescription());
        Assert.assertEquals("Tenant Profile Edge Test", tenantProfileUpdateMsg.getName());

        loginTenantAdmin();
    }
}
