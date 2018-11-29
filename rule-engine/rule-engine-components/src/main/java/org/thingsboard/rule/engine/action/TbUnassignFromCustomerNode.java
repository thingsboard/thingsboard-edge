/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ACTION,
        name = "unassign from customer",
        configClazz = TbUnassignFromCustomerNodeConfiguration.class,
        nodeDescription = "Unassign Message Originator Entity from Customer",
        nodeDetails = "Finds target Entity Customer by Customer name pattern and then unassign Originator Entity from this customer.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeUnAssignToCustomerConfig",
        icon = "remove_circle"
)
public class TbUnassignFromCustomerNode extends TbAbstractCustomerActionNode<TbUnassignFromCustomerNodeConfiguration> {

    @Override
    protected boolean createCustomerIfNotExists() {
        return false;
    }

    @Override
    protected TbUnassignFromCustomerNodeConfiguration loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbUnassignFromCustomerNodeConfiguration.class);
    }

    @Override
    protected void doProcessCustomerAction(TbContext ctx, TbMsg msg, CustomerId customerId) {
        EntityType originatorType = msg.getOriginator().getEntityType();
        switch (originatorType) {
            case DEVICE:
                processUnnasignDevice(ctx, msg);
                break;
            case ASSET:
                processUnnasignAsset(ctx, msg);
                break;
            case ENTITY_VIEW:
                processUnassignEntityView(ctx, msg);
                break;
            case DASHBOARD:
                processUnnasignDashboard(ctx, msg, customerId);
                break;
            default:
                ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + originatorType +
                        "'! Only 'DEVICE', 'ASSET',  'ENTITY_VIEW' or 'DASHBOARD' types are allowed."));
                break;
        }
    }

    private void processUnnasignAsset(TbContext ctx, TbMsg msg) {
        ctx.getAssetService().unassignAssetFromCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
    }

    private void processUnnasignDevice(TbContext ctx, TbMsg msg) {
        ctx.getDeviceService().unassignDeviceFromCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
    }

    private void processUnnasignDashboard(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDashboardService().unassignDashboardFromCustomer(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()), customerId);
    }

    private void processUnassignEntityView(TbContext ctx, TbMsg msg) {
        ctx.getEntityViewService().unassignEntityViewFromCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
    }
}
