/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RuleChainClientTest extends AbstractContainerTest {

    @Test
    public void testRuleChains() throws Exception {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRuleChains(new PageLink(100)).getTotalElements() == 1);

        PageData<RuleChain> pageData = edgeRestClient.getRuleChains(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.RULE_CHAIN);

        // create rule chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge Test Rule Chain");
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = cloudRestClient.saveRuleChain(ruleChain);
        createRuleChainMetadata(savedRuleChain);

        cloudRestClient.assignRuleChainToEdge(edge.getId(), savedRuleChain.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRuleChainById(savedRuleChain.getId()).isPresent());

        assertEntitiesByIdsAndType(Collections.singletonList(savedRuleChain.getId()), EntityType.RULE_CHAIN);

        // update rule chain
        savedRuleChain.setName("Edge Test Rule Chain Updated");
        cloudRestClient.saveRuleChain(savedRuleChain);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Test Rule Chain Updated"
                        .equals(edgeRestClient.getRuleChainById(savedRuleChain.getId()).get().getName()));

        // unassign rule chain from edge
        cloudRestClient.unassignRuleChainFromEdge(edge.getId(), savedRuleChain.getId());

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getRuleChainById(savedRuleChain.getId()).isEmpty());

        // delete rule chain
        cloudRestClient.deleteRuleChain(savedRuleChain.getId());
    }

    private void createRuleChainMetadata(RuleChain ruleChain) throws Exception {
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode1 = new RuleNode();
        ruleNode1.setName("name1");
        ruleNode1.setType("type1");
        ruleNode1.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree("\"key1\": \"val1\""));

        RuleNode ruleNode2 = new RuleNode();
        ruleNode2.setName("name2");
        ruleNode2.setType("type2");
        ruleNode2.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree("\"key2\": \"val2\""));

        RuleNode ruleNode3 = new RuleNode();
        ruleNode3.setName("name3");
        ruleNode3.setType("type3");
        ruleNode3.setConfiguration(JacksonUtil.OBJECT_MAPPER.readTree("\"key3\": \"val3\""));

        List<RuleNode> ruleNodes = new ArrayList<>();
        ruleNodes.add(ruleNode1);
        ruleNodes.add(ruleNode2);
        ruleNodes.add(ruleNode3);
        ruleChainMetaData.setFirstNodeIndex(0);
        ruleChainMetaData.setNodes(ruleNodes);

        ruleChainMetaData.addConnectionInfo(0, 1, "success");
        ruleChainMetaData.addConnectionInfo(0, 2, "fail");
        ruleChainMetaData.addConnectionInfo(1, 2, "success");

        // ruleChainMetaData.addRuleChainConnectionInfo(2, edge.getRootRuleChainId(), "success", JacksonUtil.OBJECT_MAPPER.createObjectNode());

        cloudRestClient.saveRuleChainMetaData(ruleChainMetaData);
    }
}

