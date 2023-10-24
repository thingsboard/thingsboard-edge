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
import org.junit.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class TenantProfileEdgeTest extends AbstractEdgeTest {

    @Test
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

    @Test
    public void testIsolatedTenantProfile() throws Exception {
        loginSysAdmin();

        TenantProfile edgeTenantProfile = doGet("/api/tenantProfile/" + tenantProfileId.getId(), TenantProfile.class);

        // set tenant profile isolated and add 2 queues - main and isolated
        edgeTenantProfile.setIsolatedTbRuleEngine(true);
        TenantProfileQueueConfiguration mainQueueConfiguration = createQueueConfig(DataConstants.MAIN_QUEUE_NAME, DataConstants.MAIN_QUEUE_TOPIC);
        TenantProfileQueueConfiguration isolatedQueueConfiguration = createQueueConfig("IsolatedHighPriority", "tb_rule_engine.isolated_hp");
        edgeTenantProfile.getProfileData().setQueueConfiguration(List.of(mainQueueConfiguration, isolatedQueueConfiguration));
        edgeImitator.expectMessageAmount(3);
        edgeTenantProfile = doPost("/api/tenantProfile", edgeTenantProfile, TenantProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<TenantProfileUpdateMsg> tenantProfileUpdateMsgOpt  = edgeImitator.findMessageByType(TenantProfileUpdateMsg.class);
        Assert.assertTrue(tenantProfileUpdateMsgOpt.isPresent());
        TenantProfileUpdateMsg tenantProfileUpdateMsg = tenantProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantProfileUpdateMsg.getMsgType());
        Assert.assertEquals(edgeTenantProfile.getUuidId().getMostSignificantBits(), tenantProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(edgeTenantProfile.getUuidId().getLeastSignificantBits(), tenantProfileUpdateMsg.getIdLSB());
        Assert.assertEquals(edgeTenantProfile.getDescription(), tenantProfileUpdateMsg.getDescription());

        List<QueueUpdateMsg> queueUpdateMsgs = edgeImitator.findAllMessagesByType(QueueUpdateMsg.class);
        Assert.assertEquals(2, queueUpdateMsgs.size());

        loginTenantAdmin();

        edgeImitator.expectMessageAmount(24);
        doPost("/api/edge/sync/" + edge.getId());
        assertThat(edgeImitator.waitForMessages()).as("await for messages after edge sync rest api call").isTrue();

        Assert.assertTrue(edgeImitator.getDownlinkMsgs().get(0) instanceof TenantUpdateMsg);
        Assert.assertTrue(edgeImitator.getDownlinkMsgs().get(1) instanceof TenantProfileUpdateMsg);

        queueUpdateMsgs = edgeImitator.findAllMessagesByType(QueueUpdateMsg.class);
        Assert.assertEquals(2, queueUpdateMsgs.size());
        for (QueueUpdateMsg queueUpdateMsg : queueUpdateMsgs) {
            Assert.assertEquals(tenantId.getId().getMostSignificantBits(), queueUpdateMsg.getTenantIdMSB());
            Assert.assertEquals(tenantId.getId().getLeastSignificantBits(), queueUpdateMsg.getTenantIdLSB());
        }
    }

    private TenantProfileQueueConfiguration createQueueConfig(String queueName, String queueTopic) {
        TenantProfileQueueConfiguration queueConfiguration = new TenantProfileQueueConfiguration();
        queueConfiguration.setName(queueName);
        queueConfiguration.setTopic(queueTopic);
        queueConfiguration.setPollInterval(25);
        queueConfiguration.setPartitions(10);
        queueConfiguration.setConsumerPerPartition(true);
        queueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        queueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        queueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        return queueConfiguration;
    }
}
