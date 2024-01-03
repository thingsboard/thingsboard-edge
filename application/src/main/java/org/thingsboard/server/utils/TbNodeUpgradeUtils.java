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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.service.component.RuleNodeClassInfo;

@Slf4j
public class TbNodeUpgradeUtils {

    public static void upgradeConfigurationAndVersion(RuleNode node, RuleNodeClassInfo nodeInfo) {
        JsonNode oldConfiguration = node.getConfiguration();
        int configurationVersion = node.getConfigurationVersion();

        int currentVersion = nodeInfo.getCurrentVersion();
        var configClass = nodeInfo.getAnnotation().configClazz();

        if (oldConfiguration == null || !oldConfiguration.isObject()) {
            log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                            "Current configuration is null or not a json object. " +
                            "Going to set default configuration ... ",
                    node.getId(), node.getType(), configurationVersion, currentVersion);
            node.setConfiguration(getDefaultConfig(configClass));
        } else {
            var tbVersionedNode = getTbVersionedNode(nodeInfo);
            try {
                TbPair<Boolean, JsonNode> upgradeResult = tbVersionedNode.upgrade(configurationVersion, oldConfiguration);
                if (upgradeResult.getFirst()) {
                    node.setConfiguration(upgradeResult.getSecond());
                }
            } catch (Exception e) {
                try {
                    JacksonUtil.treeToValue(oldConfiguration, configClass);
                } catch (Exception ex) {
                    log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}. " +
                                    "Going to set default configuration ... ",
                            node.getId(), node.getType(), configurationVersion, currentVersion, e);
                    node.setConfiguration(getDefaultConfig(configClass));
                }
            }
        }
        node.setConfigurationVersion(currentVersion);
    }

    @SneakyThrows
    private static TbNode getTbVersionedNode(RuleNodeClassInfo nodeInfo) {
        return (TbNode) nodeInfo.getClazz().getDeclaredConstructor().newInstance();
    }

    @SneakyThrows
    private static JsonNode getDefaultConfig(Class<? extends NodeConfiguration> configClass) {
        return JacksonUtil.valueToTree(configClass.getDeclaredConstructor().newInstance().defaultConfiguration());
    }

}
