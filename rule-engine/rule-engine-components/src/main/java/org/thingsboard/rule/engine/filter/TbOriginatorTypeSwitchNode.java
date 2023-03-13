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
package org.thingsboard.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "entity type switch",
        configClazz = EmptyNodeConfiguration.class,
        relationTypes = {"Device", "Asset", "Alarm", "Entity View", "Tenant", "Customer", "User", "Dashboard", "Rule chain",
                "Rule node", "Entity Group", "Data converter", "Integration", "Scheduler event", "Blob entity"},
        nodeDescription = "Route incoming messages by Message Originator Type",
        nodeDetails = "Routes messages to chain according to the entity type ('Device', 'Asset', etc.).",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbOriginatorTypeSwitchNode extends TbAbstractTypeSwitchNode {

    @Override
    protected String getRelationType(TbContext ctx, EntityId originator) throws TbNodeException {
        String relationType;
        EntityType originatorType = originator.getEntityType();
        switch (originatorType) {
            case TENANT:
                relationType = "Tenant";
                break;
            case CUSTOMER:
                relationType = "Customer";
                break;
            case USER:
                relationType = "User";
                break;
            case DASHBOARD:
                relationType = "Dashboard";
                break;
            case ASSET:
                relationType = "Asset";
                break;
            case DEVICE:
                relationType = "Device";
                break;
            case ENTITY_VIEW:
                relationType = "Entity View";
                break;
            case EDGE:
                relationType = "Edge";
                break;
            case RULE_CHAIN:
                relationType = "Rule chain";
                break;
            case RULE_NODE:
                relationType = "Rule node";
                break;
            case ENTITY_GROUP:
                relationType = "Entity Group";
                break;
            case CONVERTER:
                relationType = "Data converter";
                break;
            case INTEGRATION:
                relationType = "Integration";
                break;
            case SCHEDULER_EVENT:
                relationType = "Scheduler event";
                break;
            case BLOB_ENTITY:
                relationType = "Blob entity";
                break;
            case ALARM:
                relationType = "Alarm";
                break;
            default:
                throw new TbNodeException("Unsupported originator type: " + originatorType);
        }
        return relationType;
    }

}
