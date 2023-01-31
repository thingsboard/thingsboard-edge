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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "customer details",
        configClazz = TbGetCustomerDetailsNodeConfiguration.class,
        nodeDescription = "Adds fields from Customer details to the message body or metadata",
        nodeDetails = "If checkbox: <b>Add selected details to the message metadata</b> is selected, existing fields will be added to the message metadata instead of message data.<br><br>" +
                "<b>Note:</b> only Device, Asset, and Entity View type are allowed.<br><br>" +
                "If the originator of the message is not assigned to Customer, or originator type is not supported - Message will be forwarded to <b>Failure</b> chain, otherwise, <b>Success</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeEntityDetailsConfig")
public class TbGetCustomerDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetCustomerDetailsNodeConfiguration> {

    private static final String CUSTOMER_PREFIX = "customer_";

    @Override
    protected TbGetCustomerDetailsNodeConfiguration loadGetEntityDetailsNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetCustomerDetailsNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg) {
        return getTbMsgListenableFuture(ctx, msg, getDataAsJson(msg), CUSTOMER_PREFIX);
    }

    @Override
    protected ListenableFuture<ContactBased> getContactBasedListenableFuture(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(getCustomer(ctx, msg), customer -> {
            if (customer != null) {
                return Futures.immediateFuture(customer);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Customer> getCustomer(TbContext ctx, TbMsg msg) {
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId())), device -> {
                    if (device != null) {
                        if (!device.getCustomerId().isNullUid()) {
                            return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), device.getCustomerId());
                        } else {
                            throw new RuntimeException("Device with name '" + device.getName() + "' is not assigned to Customer.");
                        }
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, MoreExecutors.directExecutor());
            case ASSET:
                return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(msg.getOriginator().getId())), asset -> {
                    if (asset != null) {
                        if (!asset.getCustomerId().isNullUid()) {
                            return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), asset.getCustomerId());
                        } else {
                            throw new RuntimeException("Asset with name '" + asset.getName() + "' is not assigned to Customer.");
                        }
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, MoreExecutors.directExecutor());
            case ENTITY_VIEW:
                return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId())), entityView -> {
                    if (entityView != null) {
                        if (!entityView.getCustomerId().isNullUid()) {
                            return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), entityView.getCustomerId());
                        } else {
                            throw new RuntimeException("EntityView with name '" + entityView.getName() + "' is not assigned to Customer.");
                        }
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, MoreExecutors.directExecutor());
            case USER:
                return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(msg.getOriginator().getId())), user -> {
                    if (user != null) {
                        if (!user.getCustomerId().isNullUid()) {
                            return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), user.getCustomerId());
                        } else {
                            throw new RuntimeException("User with name '" + user.getName() + "' is not assigned to Customer.");
                        }
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, MoreExecutors.directExecutor());
            case EDGE:
                return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId())), edge -> {
                    if (edge != null) {
                        if (!edge.getCustomerId().isNullUid()) {
                            return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), edge.getCustomerId());
                        } else {
                            throw new RuntimeException("Edge with name '" + edge.getName() + "' is not assigned to Customer.");
                        }
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, MoreExecutors.directExecutor());
            case CUSTOMER:
                return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(msg.getOriginator().getId())), customer -> {
                    if (customer != null) {
                        if (customer.isSubCustomer()) {
                            return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), customer.getParentCustomerId());
                        } else {
                            throw new RuntimeException("Customer with title '" + customer.getTitle() + "' doesn't have parent customer.");
                        }
                    } else {
                        return Futures.immediateFuture(null);
                    }
                }, MoreExecutors.directExecutor());
            default:
                throw new RuntimeException("Entity with entityType '" + msg.getOriginator().getEntityType() + "' is not supported.");
        }
    }

}
