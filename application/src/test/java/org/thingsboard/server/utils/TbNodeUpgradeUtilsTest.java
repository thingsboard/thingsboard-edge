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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.node.NullNode;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNode;
import org.thingsboard.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.thingsboard.rule.engine.metadata.TbGetCustomerAttributeNode;
import org.thingsboard.rule.engine.metadata.TbGetEntityDataNodeConfiguration;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TbNodeUpgradeUtilsTest {

    @Test
    public void testUpgradeRuleNodeConfigurationWithNullConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);
    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNullNodeConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        node.setConfiguration(NullNode.instance);
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);
    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNonNullConfig() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetAttributesNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetAttributesNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        String versionZeroDefaultConfigStr = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        node.setConfiguration(JacksonUtil.toJsonNode(versionZeroDefaultConfigStr));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithNewConfigAndOldConfigVersion() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetEntityDataNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetCustomerAttributeNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        String versionOneDefaultConfig = "{\"fetchTo\":\"METADATA\"," +
                "\"dataMapping\":{\"alarmThreshold\":\"threshold\"}," +
                "\"dataToFetch\":\"ATTRIBUTES\"}";
        node.setConfiguration(JacksonUtil.toJsonNode(versionOneDefaultConfig));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

    @Test
    public void testUpgradeRuleNodeConfigurationWithInvalidConfigAndOldConfigVersion() throws Exception {
        // GIVEN
        var node = new RuleNode();
        var nodeInfo = mock(RuleNodeClassInfo.class);
        var nodeConfigClazz = TbGetEntityDataNodeConfiguration.class;
        var annotation = mock(org.thingsboard.rule.engine.api.RuleNode.class);
        var defaultConfig = JacksonUtil.valueToTree(nodeConfigClazz.getDeclaredConstructor().newInstance().defaultConfiguration());

        when(nodeInfo.getClazz()).thenReturn((Class) TbGetCustomerAttributeNode.class);
        when(nodeInfo.getCurrentVersion()).thenReturn(1);
        when(nodeInfo.getAnnotation()).thenReturn(annotation);
        when(annotation.configClazz()).thenReturn((Class) nodeConfigClazz);

        // missing telemetry field
        String oldConfig = "{\"attrMapping\":{\"alarmThreshold\":\"threshold\"}}";;
        node.setConfiguration(JacksonUtil.toJsonNode(oldConfig));
        // WHEN
        TbNodeUpgradeUtils.upgradeConfigurationAndVersion(node, nodeInfo);
        // THEN
        Assertions.assertThat(node.getConfiguration()).isEqualTo(defaultConfig);
        Assertions.assertThat(node.getConfigurationVersion()).isEqualTo(1);

    }

}
