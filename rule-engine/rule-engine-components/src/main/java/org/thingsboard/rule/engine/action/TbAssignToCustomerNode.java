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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "assign to customer",
        configClazz = TbAssignToCustomerNodeConfiguration.class,
        nodeDescription = "Assign Message Originator Entity to Customer",
        nodeDetails = "Finds target Customer by customer name pattern and then assign Originator Entity to this customer. " +
                "Will create new Customer if it doesn't exists and 'Create new Customer if not exists' is set to true.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAssignToCustomerConfig",
        icon = "add_circle"
)
public class TbAssignToCustomerNode extends TbAbstractCustomerActionNode<TbAssignToCustomerNodeConfiguration> {

    @Override
    protected boolean createCustomerIfNotExists() {
        return config.isCreateCustomerIfNotExists();
    }

    @Override
    protected TbAssignToCustomerNodeConfiguration loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAssignToCustomerNodeConfiguration.class);
    }

    @Override
    protected void doProcessCustomerAction(TbContext ctx, TbMsg msg, CustomerId customerId) {
        processAssign(ctx, msg, customerId);
    }

    private void processAssign(TbContext ctx, TbMsg msg, CustomerId customerId) {
        EntityType originatorType = msg.getOriginator().getEntityType();
        switch (originatorType) {
            case DEVICE:
                processAssignDevice(ctx, msg, customerId);
                break;
            case ASSET:
                processAssignAsset(ctx, msg, customerId);
                break;
            case ENTITY_VIEW:
                processAssignEntityView(ctx, msg, customerId);
                break;
            case DASHBOARD:
                processAssignDashboard(ctx, msg, customerId);
                break;
            default:
                ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + originatorType +
                        "'! Only 'DEVICE', 'ASSET',  'ENTITY_VIEW' or 'DASHBOARD' types are allowed."));
                break;
        }
    }

    private void processAssignAsset(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getAssetService().assignAssetToCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()), customerId);
    }

    private void processAssignDevice(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDeviceService().assignDeviceToCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()), customerId);
    }

    private void processAssignEntityView(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getEntityViewService().assignEntityViewToCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()), customerId);
    }

    private void processAssignDashboard(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDashboardService().assignDashboardToCustomer(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()), customerId);
    }

}
