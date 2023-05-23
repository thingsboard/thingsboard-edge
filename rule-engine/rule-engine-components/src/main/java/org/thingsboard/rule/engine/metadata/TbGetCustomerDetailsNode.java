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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.NoSuchElementException;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "customer details",
        configClazz = TbGetCustomerDetailsNodeConfiguration.class,
        nodeDescription = "Adds message originator customer details into message or message metadata",
        nodeDetails = "Useful in multi-customer solutions where we need dynamically use customer contact information " +
                "such as email, phone, address, etc., for notifications via email, SMS, and other notification providers.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeEntityDetailsConfig")
public class TbGetCustomerDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetCustomerDetailsNodeConfiguration, CustomerId> {

    private static final String CUSTOMER_PREFIX = "customer_";

    @Override
    protected TbGetCustomerDetailsNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetCustomerDetailsNodeConfiguration.class);
        checkIfDetailsListIsNotEmptyOrElseThrow(config.getDetailsList());
        return config;
    }

    @Override
    protected String getPrefix() {
        return CUSTOMER_PREFIX;
    }

    @Override
    protected ListenableFuture<Customer> getContactBasedFuture(TbContext ctx, TbMsg msg) {
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId())),
                        device -> getCustomerFuture(ctx, device, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case ASSET:
                return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(msg.getOriginator().getId())),
                        asset -> getCustomerFuture(ctx, asset, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case ENTITY_VIEW:
                return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId())),
                        entityView -> getCustomerFuture(ctx, entityView, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case USER:
                return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(msg.getOriginator().getId())),
                        user -> getCustomerFuture(ctx, user, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case EDGE:
                return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId())),
                        edge -> getCustomerFuture(ctx, edge, msg.getOriginator()), ctx.getDbCallbackExecutor());
            case CUSTOMER:
                return Futures.transformAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), new CustomerId(msg.getOriginator().getId())),
                        customer -> getCustomerFuture(ctx, customer, msg.getOriginator()), ctx.getDbCallbackExecutor());
            default:
                return Futures.immediateFailedFuture(new NoSuchElementException("Entity with entityType '" + msg.getOriginator().getEntityType() + "' is not supported."));
        }
    }

    private ListenableFuture<Customer> getCustomerFuture(TbContext ctx, HasCustomerId hasCustomerId, EntityId originator) {
        if (hasCustomerId == null) {
            return Futures.immediateFuture(null);
        } else {
            if (hasCustomerId.getCustomerId() == null || hasCustomerId.getCustomerId().isNullUid()) {
                if (hasCustomerId instanceof HasName) {
                    var hasName = (HasName) hasCustomerId;
                    boolean isCustomer = hasName instanceof Customer;
                    var errorMsg = isCustomer ?
                            "' doesn't have parent customer!" :
                            "' is not assigned to Customer!";
                    var withName = isCustomer ?
                            " with title '" :
                            " with name '";
                    throw new RuntimeException(originator.getEntityType().getNormalName() + withName + hasName.getName() + errorMsg);
                }
                throw new RuntimeException(originator.getEntityType().getNormalName() + " with id '" + originator + "' is not assigned to Customer!");
            } else {
                return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), hasCustomerId.getCustomerId());
            }
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "addToMetadata",
                        FetchTo.METADATA.name(),
                        FetchTo.DATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }

}
