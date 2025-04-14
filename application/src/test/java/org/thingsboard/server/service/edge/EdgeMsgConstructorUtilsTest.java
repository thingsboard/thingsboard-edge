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
package org.thingsboard.server.service.edge;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbChangeOwnerNode;
import org.thingsboard.rule.engine.action.TbChangeOwnerNodeConfiguration;
import org.thingsboard.rule.engine.action.TbChangeOwnerNode;
import org.thingsboard.rule.engine.action.TbSaveToCustomCassandraTableNode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.aws.lambda.TbAwsLambdaNode;
import org.thingsboard.rule.engine.filter.TbCheckRelationNode;
import org.thingsboard.rule.engine.flow.TbAckNode;
import org.thingsboard.rule.engine.math.TbMathNode;
import org.thingsboard.rule.engine.metadata.CalculateDeltaNode;
import org.thingsboard.rule.engine.metadata.TbGetTelemetryNode;
import org.thingsboard.rule.engine.rest.TbSendRestApiCallReplyNode;
import org.thingsboard.rule.engine.telemetry.TbCalculatedFieldsNode;
import org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.thingsboard.server.service.edge.EdgeMsgConstructorUtils.EXCLUDED_NODES_BY_EDGE_VERSION;
import static org.thingsboard.server.service.edge.EdgeMsgConstructorUtils.IGNORED_PARAMS_BY_EDGE_VERSION;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EdgeMsgConstructorUtilsTest {

    private static final int CONFIGURATION_VERSION = 5;

    static Stream<EdgeVersion> provideEdgeVersions() {
        return Stream.of(
                EdgeVersion.V_4_0_0,
                EdgeVersion.V_3_9_0,
                EdgeVersion.V_3_8_0,
                EdgeVersion.V_3_7_0
        );
    }

    private static final RuleChainMetaData RULE_CHAIN_META_DATA = new RuleChainMetaData();
    private static final List<TbNode> TEST_NODES =
            List.of(
                    new TbSaveToCustomCassandraTableNode(),
                    new TbMsgAttributesNode(),
                    new TbMsgTimeseriesNode(),
                    new TbChangeOwnerNode(),
                    new TbSendRestApiCallReplyNode(),
                    new TbAwsLambdaNode(),
                    new TbCalculatedFieldsNode(),

                    new TbMathNode(),
                    new CalculateDeltaNode(),
                    new TbAckNode(),
                    new TbCheckRelationNode(),
                    new TbGetTelemetryNode()
            );

    @BeforeAll
    static void setUp() {
        List<RuleNode> ruleNodes = TEST_NODES.stream()
                .map(node -> {
                    RuleNode ruleNode = new RuleNode();
                    ruleNode.setName(node.getClass().getName());
                    ruleNode.setType(node.getClass().getName());
                    ruleNode.setConfigurationVersion(CONFIGURATION_VERSION);
                    ruleNode.setConfiguration(JacksonUtil.valueToTree(createDefaultConfiguration(node)));
                    return ruleNode;
                })
                .toList();

        RULE_CHAIN_META_DATA.setFirstNodeIndex(0);
        RULE_CHAIN_META_DATA.setNodes(ruleNodes);
    }

    private static NodeConfiguration<?> createDefaultConfiguration(TbNode node) {
        try {
            org.thingsboard.rule.engine.api.RuleNode annotation = node.getClass().getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class);
            Constructor<?> constructor = annotation.configClazz().getConstructor();
            NodeConfiguration<?> configInstance = (NodeConfiguration<?>) constructor.newInstance();

            return configInstance.defaultConfiguration();
        } catch (Exception e) {
            throw new RuntimeException("Exception during creating RuleNodeConfiguration for node - " + node, e);
        }
    }

    @ParameterizedTest(name = "Test Sanitize Metadata For Edge: {0}")
    @MethodSource("provideEdgeVersions")
    @DisplayName("Test Sanitize Metadata For Legacy Edge Version")
    public void testSanitizeMetadataForLegacyEdgeVersion(EdgeVersion edgeVersion) {
        // WHEN
        List<RuleNode> ruleNodes = sanitizeMetadataForLegacyEdgeVersion(edgeVersion);

        // THEN
        ruleNodes.forEach(ruleNode -> {
            checkUpdateNodeConfigurationsForLegacyEdge(ruleNode, edgeVersion);
            checkRemoveExcludedNodesForLegacyEdge(ruleNode, edgeVersion);
        });
    }

    private List<RuleNode> sanitizeMetadataForLegacyEdgeVersion(EdgeVersion edgeVersion) {
        String metadataUpdateMsg = EdgeMsgConstructorUtils.constructRuleChainMetadataUpdatedMsg(
                UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE,
                RULE_CHAIN_META_DATA,
                edgeVersion
        ).getEntity();

        RuleChainMetaData updatedMetaData = JacksonUtil.fromString(metadataUpdateMsg, RuleChainMetaData.class, true);
        Assertions.assertNotNull(updatedMetaData, "RuleChainMetaData should not be null after update.");

        return updatedMetaData.getNodes();
    }

    private void checkUpdateNodeConfigurationsForLegacyEdge(RuleNode ruleNode, EdgeVersion edgeVersion) {
        if (IGNORED_PARAMS_BY_EDGE_VERSION.containsKey(edgeVersion) && IGNORED_PARAMS_BY_EDGE_VERSION.get(edgeVersion).containsKey(ruleNode.getType())) {
            String ignoredParam = IGNORED_PARAMS_BY_EDGE_VERSION.get(edgeVersion).get(ruleNode.getType());

            Assertions.assertFalse(ruleNode.getConfiguration().has(ignoredParam),
                    String.format("RuleNode '%s' for EdgeVersion '%s' should ignore '%s' config parameter.", ruleNode.getName(), edgeVersion, ignoredParam));
        }
    }

    private void checkRemoveExcludedNodesForLegacyEdge(RuleNode ruleNode, EdgeVersion edgeVersion) {
        boolean isNodeExcluded = Optional.ofNullable(EXCLUDED_NODES_BY_EDGE_VERSION.get(edgeVersion))
                .map(excludedNodes -> !excludedNodes.contains(ruleNode.getType()))
                .orElse(true);

        Assertions.assertTrue(isNodeExcluded,
                String.format("For EdgeVersion '%s', ruleNode '%s' should not be included.", edgeVersion, ruleNode.getType()));
    }

}
