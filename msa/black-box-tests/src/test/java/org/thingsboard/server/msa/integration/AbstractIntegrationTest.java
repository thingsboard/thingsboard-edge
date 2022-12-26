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
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.NodeConnectionInfo;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public abstract class AbstractIntegrationTest extends AbstractContainerTest {
    protected static final String LOGIN = "tenant@thingsboard.org";
    protected static final String PASSWORD = "tenant";
    protected Device device;
    protected Integration integration;

    abstract protected String getDevicePrototypeSufix();

    @BeforeClass
    public void beforeIntegrationTestClass() {
        testRestClient.login(LOGIN, PASSWORD);
    }

    @BeforeMethod
    public void beforeIntegrationTest() {
        device = testRestClient.postDevice("", defaultDevicePrototype(getDevicePrototypeSufix()));
    }

    @AfterMethod
    public void afterIntegrationTest() {
        if (device != null){
            testRestClient.deleteDevice(device.getId());
            testRestClient.deleteIntegration(integration.getId());
            testRestClient.deleteConverter(integration.getDefaultConverterId());
            if (integration.getDownlinkConverterId() != null) {
                testRestClient.deleteConverter(integration.getDownlinkConverterId());
            }
        }
    }

    protected Integration createIntegration(IntegrationType type, String config, JsonNode converterConfig,
                                            String routingKey, String secretKey, boolean isRemote) {
        Integration integration = new Integration();
        JsonNode conf = JacksonUtil.toJsonNode(config);
        integration.setConfiguration(conf);
        integration.setDefaultConverterId(createUplink(converterConfig).getId());
        integration.setName(type.name().toLowerCase());
        integration.setType(type);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secretKey);
        integration.setEnabled(true);
        integration.setRemote(isRemote);
        integration.setDebugMode(true);
        integration.setAllowCreateDevicesOrAssets(true);

        integration = testRestClient.postIntegration(integration);

        IntegrationId integrationId = integration.getId();
        TenantId tenantId = integration.getTenantId();

        waitUntilIntegrationStarted(integrationId, tenantId);
        return integration;
    }
    protected void waitUntilIntegrationStarted(IntegrationId integrationId, TenantId tenantId) {
        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(integrationId, EventType.LC_EVENT, tenantId, new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    EventInfo event = events.getData().stream().max(Comparator.comparingLong(EventInfo::getCreatedTime)).orElse(null);
                    return event != null
                            && "STARTED".equals(event.getBody().get("event").asText())
                            && "true".equals(event.getBody().get("success").asText());
                });
    }

    protected void waitForIntegrationEvent(Integration integration, String eventType, int count) {
        if (containerTestSuite.isActive() && !integration.getType().isSingleton() && !integration.isRemote()) {
            count = count * 2;
        }
        int finalCount = count;
        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(integration.getId(), EventType.LC_EVENT, integration.getTenantId(), new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    List<EventInfo> eventInfos = events.getData().stream().filter(eventInfo ->
                                    eventType.equals(eventInfo.getBody().get("event").asText()) &&
                                            "true".equals(eventInfo.getBody().get("success").asText()))
                            .collect(Collectors.toList());

                    return eventInfos.size() == finalCount;
                });
    }
    protected RuleChainId getDefaultRuleChainId() {
        PageData<RuleChain> ruleChains = testRestClient.getRuleChains(new PageLink(40, 0));
        Optional<RuleChain> defaultRuleChain = ruleChains.getData()
                .stream()
                .filter(RuleChain::isRoot)
                .findFirst();
        if (!defaultRuleChain.isPresent()) {
            Assert.fail("Root rule chain wasn't found");
        }
        return defaultRuleChain.get().getId();
    }

    protected RuleChainId createRootRuleChainWithIntegrationDownlinkNode(IntegrationId integrationId) throws Exception {
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setName("testRuleChain");
        RuleChain ruleChain = testRestClient.saveRuleChain(newRuleChain);

        JsonNode configuration = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream("DownlinkRuleChainMetadata.json"));
        RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainMetaData.setFirstNodeIndex(configuration.get("firstNodeIndex").asInt());
        ruleChainMetaData.setNodes(Arrays.asList(mapper.treeToValue(configuration.get("nodes"), RuleNode[].class)));
        RuleNode integrationNode  = ruleChainMetaData.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
        integrationNode.setConfiguration(mapper.createObjectNode().put("integrationId", integrationId.toString()));
        ruleChainMetaData.setConnections(Arrays.asList(mapper.treeToValue(configuration.get("connections"), NodeConnectionInfo[].class)));

        testRestClient.postRuleChainMetadata(ruleChainMetaData);

        // make rule chain root
        testRestClient.setRootRuleChain(ruleChain.getId());
        return ruleChain.getId();
    }

    protected void waitTillRuleNodeReceiveMsg(EntityId entityId, EventType eventType, TenantId tenantId, String msgType) {
        Awaitility
                .await()
                .alias("Get events from rule node")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = testRestClient.getEvents(entityId, eventType, tenantId, new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    EventInfo event = events.getData().stream().max(Comparator.comparingLong(EventInfo::getCreatedTime)).orElse(null);
                    return event != null
                            && msgType.equals(event.getBody().get("msgType").asText());
                });
    }
}
