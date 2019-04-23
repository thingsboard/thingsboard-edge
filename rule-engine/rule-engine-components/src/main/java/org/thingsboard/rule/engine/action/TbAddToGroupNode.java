/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "add to group",
        configClazz = TbAddToGroupConfiguration.class,
        nodeDescription = "Adds Message Originator Entity to Entity Group",
        nodeDetails = "Finds target Entity Group by group name pattern and then adds Originator Entity to this group. " +
                "Will create new Entity Group if it doesn't exists and 'Create new group if not exists' is set to true.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAddToGroupConfig",
        icon = "add_circle"
)
public class TbAddToGroupNode extends TbAbstractGroupActionNode<TbAddToGroupConfiguration> {

    @Override
    protected boolean createGroupIfNotExists() {
        return config.isCreateGroupIfNotExists();
    }

    @Override
    protected TbAddToGroupConfiguration loadGroupNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAddToGroupConfiguration.class);
    }

    @Override
    protected void doProcessEntityGroupAction(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        if (config.isRemoveFromCurrentGroups()) {
            removeFromCurrentGroups(ctx, msg, entityGroupId);
        }
        addEntityToGroup(ctx, msg, entityGroupId);
    }

    private void removeFromCurrentGroups(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        DonAsynchron.withCallback(ctx.getPeContext().getEntityGroupService()
                .findEntityGroupsForEntity(ctx.getTenantId(), msg.getOriginator()), entityGroupIds -> {
            if (!entityGroupIds.isEmpty()) {
                for (EntityGroupId groupId : entityGroupIds) {
                    if (!groupId.equals(entityGroupId)) {
                        ctx.getPeContext().getEntityGroupService()
                                .removeEntityFromEntityGroup(ctx.getTenantId(), groupId, msg.getOriginator());
                    }
                }
            }
        }, throwable -> {
            throw new RuntimeException(throwable);
        });
    }

    private void addEntityToGroup(TbContext ctx, TbMsg msg, EntityGroupId entityGroupId) {
        ctx.getPeContext().getEntityGroupService().addEntityToEntityGroup(ctx.getTenantId(), entityGroupId, msg.getOriginator());
    }
}


