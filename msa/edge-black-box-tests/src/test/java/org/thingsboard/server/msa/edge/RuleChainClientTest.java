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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.Collections;
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
        RuleChainId savedRuleChainId = createRuleChainAndAssignToEdge("Edge Test Rule Chain");

        assertEntitiesByIdsAndType(Collections.singletonList(savedRuleChainId), EntityType.RULE_CHAIN);

        // update rule chain
        RuleChain savedRuleChain = cloudRestClient.getRuleChainById(savedRuleChainId).get();
        savedRuleChain.setName("Edge Test Rule Chain Updated");
        cloudRestClient.saveRuleChain(savedRuleChain);
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> "Edge Test Rule Chain Updated"
                        .equals(edgeRestClient.getRuleChainById(savedRuleChainId).get().getName()));

        unAssignFromEdgeAndDeleteRuleChain(savedRuleChainId);
    }
}

