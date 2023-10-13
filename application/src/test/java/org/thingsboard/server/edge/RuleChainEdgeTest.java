/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RuleChainEdgeTest extends AbstractEdgeTest {

    private static final int CONFIGURATION_VERSION = 5;

    @Test
    @Ignore
    public void testRuleChains() throws Exception {
        // create rule chain
        edgeImitator.expectMessageAmount(2);
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        createRuleChainMetadata(savedRuleChain);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        RuleChain ruleChainMsg = JacksonUtil.fromStringIgnoreUnknownProperties(ruleChainUpdateMsg.getEntity(), RuleChain.class);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertTrue(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()) ||
                UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE.equals(ruleChainUpdateMsg.getMsgType()));
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());
        Assert.assertEquals(savedRuleChain.getName(), ruleChainMsg.getName());

        testRuleChainMetadataRequestMsg(savedRuleChain.getId());

        // unassign rule chain from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertEquals(ruleChainUpdateMsg.getIdMSB(), savedRuleChain.getUuidId().getMostSignificantBits());
        Assert.assertEquals(ruleChainUpdateMsg.getIdLSB(), savedRuleChain.getUuidId().getLeastSignificantBits());

        // delete rule chain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));
    }

    @Test
    @Ignore
    public void testSendRuleChainMetadataRequestToCloud() throws Exception {
        RuleChainId edgeRootRuleChainId = edge.getRootRuleChainId();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RuleChainMetadataRequestMsg.Builder ruleChainMetadataRequestMsgBuilder = RuleChainMetadataRequestMsg.newBuilder();
        ruleChainMetadataRequestMsgBuilder.setRuleChainIdMSB(edgeRootRuleChainId.getId().getMostSignificantBits());
        ruleChainMetadataRequestMsgBuilder.setRuleChainIdLSB(edgeRootRuleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(ruleChainMetadataRequestMsgBuilder);
        uplinkMsgBuilder.addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
        RuleChainMetaData ruleChainMetadataMsg = JacksonUtil.fromStringIgnoreUnknownProperties(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class);
        Assert.assertNotNull(ruleChainMetadataMsg);
        Assert.assertEquals(edgeRootRuleChainId, ruleChainMetadataMsg.getRuleChainId());

        testAutoGeneratedCodeByProtobuf(ruleChainMetadataUpdateMsg);
    }

    private void testRuleChainMetadataRequestMsg(RuleChainId ruleChainId) throws Exception {
        RuleChainMetadataRequestMsg.Builder ruleChainMetadataRequestMsgBuilder = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(ruleChainMetadataRequestMsgBuilder);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addRuleChainMetadataRequestMsg(ruleChainMetadataRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RuleChainMetadataUpdateMsg);
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = (RuleChainMetadataUpdateMsg) latestMessage;
        RuleChainMetaData ruleChainMetadataMsg = JacksonUtil.fromStringIgnoreUnknownProperties(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class);
        Assert.assertNotNull(ruleChainMetadataMsg);
        Assert.assertEquals(ruleChainId, ruleChainMetadataMsg.getRuleChainId());

        for (RuleNode ruleNode : ruleChainMetadataMsg.getNodes()) {
            Assert.assertEquals(CONFIGURATION_VERSION, ruleNode.getConfigurationVersion());
        }
    }

    private void createRuleChainMetadata(RuleChain ruleChain) {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode1.setConfigurationVersion(CONFIGURATION_VERSION);
        TbGetAttributesNodeConfiguration configuration = new TbGetAttributesNodeConfiguration();
        configuration.setFetchTo(TbMsgSource.METADATA);
        configuration.setServerAttributeNames(Collections.singletonList("serverAttributeKey2"));
        ruleNode1.setConfiguration(JacksonUtil.valueToTree(configuration));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode2.setConfigurationVersion(CONFIGURATION_VERSION);
        ruleNode2.setConfiguration(JacksonUtil.valueToTree(configuration));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode3.setConfigurationVersion(CONFIGURATION_VERSION);
        ruleNode3.setConfiguration(JacksonUtil.valueToTree(configuration));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0, 1, "success");
        ruleChainMetaData.addConnectionInfo(0, 2, "fail");
        ruleChainMetaData.addConnectionInfo(1, 2, "success");

        doPost("/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class);
    }

    @Test
    @Ignore
    public void testSetRootRuleChain() throws Exception {
        // create rule chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge New Root Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // set new rule chain as root
        RuleChainId currentRootRuleChainId = edge.getRootRuleChainId();
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/" + savedRuleChain.getUuidId() + "/root", Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        RuleChain ruleChainMsg = JacksonUtil.fromStringIgnoreUnknownProperties(ruleChainUpdateMsg.getEntity(), RuleChain.class);
        Assert.assertNotNull(ruleChainMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, ruleChainUpdateMsg.getMsgType());
        Assert.assertTrue(ruleChainMsg.isRoot());
        Assert.assertEquals(savedRuleChain.getId(), ruleChainMsg.getId());

        // revert root rule chain
        edgeImitator.expectMessageAmount(1);
        doPost("/api/edge/" + edge.getUuidId()
                + "/" + currentRootRuleChainId.getId() + "/root", Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // unassign rule chain from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // delete rule chain
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/ruleChain/" + savedRuleChain.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(5));
    }
}
