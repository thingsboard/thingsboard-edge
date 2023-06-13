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
package org.thingsboard.server.rules.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.FetchTo;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.controller.AbstractRuleEngineControllerTest;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.event.EventService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractRuleEngineLifecycleIntegrationTest extends AbstractRuleEngineControllerTest {

    @Autowired
    protected ActorSystemContext actorSystem;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected EventService eventService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testRuleChainWithOneRule() throws Exception {
        // Creating Rule Chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Simple Rule Chain");
        ruleChain.setTenantId(tenantId);
        ruleChain.setRoot(true);
        ruleChain.setDebugMode(true);
        ruleChain = saveRuleChain(ruleChain);
        Assert.assertNull(ruleChain.getFirstRuleNodeId());

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("Simple Rule Node");
        ruleNode.setType(org.thingsboard.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode.setConfigurationVersion(TbGetAttributesNode.class.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version());
        ruleNode.setDebugMode(true);
        TbGetAttributesNodeConfiguration configuration = new TbGetAttributesNodeConfiguration();
        configuration.setFetchTo(FetchTo.METADATA);
        configuration.setServerAttributeNames(Collections.singletonList("serverAttributeKey"));
        ruleNode.setConfiguration(JacksonUtil.valueToTree(configuration));

        metaData.setNodes(Collections.singletonList(ruleNode));
        metaData.setFirstNodeIndex(0);

        metaData = saveRuleChainMetaData(metaData);
        Assert.assertNotNull(metaData);

        final RuleChain ruleChainFinal = getRuleChain(ruleChain.getId());
        Assert.assertNotNull(ruleChainFinal.getFirstRuleNodeId());

        //TODO find out why RULE_NODE update event did not appear all the time
        List<EventInfo> rcEvents = Awaitility.await("Rule Node started successfully")
                .pollInterval(10, MILLISECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                            List<EventInfo> debugEvents = getEvents(tenantId, ruleChainFinal.getFirstRuleNodeId(), EventType.LC_EVENT.getOldName(), 1000)
                                    .getData().stream().filter(e -> {
                                        var body = e.getBody();
                                        return body.has("event") && body.get("event").asText().equals("STARTED")
                                                && body.has("success") && body.get("success").asBoolean();
                                    }).collect(Collectors.toList());
                            debugEvents.forEach((e) -> log.trace("event: {}", e));
                            return debugEvents;
                        },
                        x -> x.size() == 1);

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        log.warn("before update attr");
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey", "serverAttributeValue"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);
        log.warn("attr updated");
        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        Mockito.when(tbMsgCallback.isMsgValid()).thenReturn(true);
        TbMsg tbMsg = TbMsg.newMsg("CUSTOM", device.getId(), new TbMsgMetaData(), "{}", tbMsgCallback);
        QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(tenantId, tbMsg, null, null);
        // Pushing Message to the system
        log.warn("before tell tbMsgCallback");
        actorSystem.tell(qMsg);
        log.warn("awaiting tbMsgCallback");
        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();
        log.warn("awaiting events");
        List<EventInfo> events = Awaitility.await("get debug by custom event")
                .pollInterval(10, MILLISECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                            List<EventInfo> debugEvents = getDebugEvents(tenantId, ruleChainFinal.getFirstRuleNodeId(), 1000)
                                    .getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
                            log.warn("filtered debug events [{}]", debugEvents.size());
                            debugEvents.forEach((e) -> log.warn("event: {}", e));
                            return debugEvents;
                        },
                        x -> x.size() == 2);
        log.warn("asserting..");

        EventInfo inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(ruleChainFinal.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        EventInfo outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(ruleChainFinal.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        log.warn("OUT event {}", outEvent);
        log.warn("OUT event metadata {}", getMetadata(outEvent));

        Assert.assertNotNull("metadata has ss_serverAttributeKey", getMetadata(outEvent).get("ss_serverAttributeKey"));
        Assert.assertEquals("serverAttributeValue", getMetadata(outEvent).get("ss_serverAttributeKey").asText());
    }

}
