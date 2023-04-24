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
package org.thingsboard.server.service;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.service.validator.RuleChainDataValidator;
import org.thingsboard.server.exception.DataValidationException;

public class RuleChainDataValidatorTest {

    @Test
    public void testSingletonSupport() {
        String node = "org.thingsboard.rule.engine.mqtt.TbMqttNode";
        RuleNode ruleNode = createRuleNode(node, false);
        RuleChainDataValidator.validateRuleNode(ruleNode);
        ruleNode.setSingletonMode(true);
        RuleChainDataValidator.validateRuleNode(ruleNode);
    }

    @Test
    public void testSingletonNotSupport() {
        String node = "org.thingsboard.rule.engine.flow.TbAckNode";
        RuleNode ruleNode = createRuleNode(node, false);
        RuleChainDataValidator.validateRuleNode(ruleNode);
        ruleNode.setSingletonMode(true);
        Assertions.assertThrows(DataValidationException.class,
                () -> RuleChainDataValidator.validateRuleNode(ruleNode),
                String.format("Singleton mode not supported for [%s].", ruleNode.getType()));
    }

    @Test
    public void testSingletonOnly() {
        String node = "org.thingsboard.rule.engine.mqtt.azure.TbAzureIotHubNode";
        RuleNode ruleNode = createRuleNode(node, true);
        RuleChainDataValidator.validateRuleNode(ruleNode);
        ruleNode.setSingletonMode(false);
        Assertions.assertThrows(DataValidationException.class,
                () -> RuleChainDataValidator.validateRuleNode(ruleNode),
                String.format("Supported only singleton mode for [%s].", ruleNode.getType()));
    }

    private RuleNode createRuleNode(String type, boolean singletonMode) {
        RuleNode ruleNode = new RuleNode();
        ruleNode.setName("test node");
        ruleNode.setType(type);
        ruleNode.setSingletonMode(singletonMode);
        ruleNode.setConfiguration(JacksonUtil.newObjectNode());
        return ruleNode;
    }
}
