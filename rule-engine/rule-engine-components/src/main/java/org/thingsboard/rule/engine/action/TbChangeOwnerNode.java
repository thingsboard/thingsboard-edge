/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "change owner",
        configClazz = TbChangeOwnerNodeConfiguration.class,
        version = 1,
        nodeDescription = "Changes Owner of the Originator entity to the selected Owner by type (Tenant, Customer)." +
                "If selected Owner type - <b>Customer:</b></br>" +
                "Rule node finds target Owner by owner name pattern and then change the owner of the originator entity.</br>" +
                "Rule node can create new Owner if it doesn't exist and selected checkbox 'Create new owner if not exists'.",
        nodeDetails = "If an entity already belongs to this owner or entity owner is successfully changed - " +
                "Message sent via <b>Success</b> chain, otherwise, <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeChangeOwnerConfig",
        icon = "assignment_ind"
)
public class TbChangeOwnerNode implements TbNode {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.TENANT, EntityType.CUSTOMER);
    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(", "));

    private TbChangeOwnerNodeConfiguration config;
    private EntityType ownerType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbChangeOwnerNodeConfiguration.class);
        ownerType = config.getOwnerType();
        if (ownerType == null) {
            throw new TbNodeException("Owner type should be specified!", true);
        }
        if (!supportedEntityTypes.contains(ownerType)) {
            throw new TbNodeException(unsupportedOwnerTypeErrorMessage(), true);
        }
        if (!EntityType.CUSTOMER.equals(ownerType)) {
            return;
        }
        if (StringUtils.isBlank(config.getOwnerNamePattern())) {
            throw new TbNodeException("Owner name should be specified!", true);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        EntityId originator = msg.getOriginator();
        ListenableFuture<Void> changeOwnerFuture;
        switch (ownerType) {
            case TENANT -> changeOwnerFuture = changeOwnerAsync(ctx, originator, ctx.getTenantId());
            case CUSTOMER -> {
                String ownerName = TbNodeUtils.processPattern(config.getOwnerNamePattern(), msg);
                ListenableFuture<CustomerId> customerIdFuture = findOrCreateCustomer(ctx, msg, ownerName);
                changeOwnerFuture = Futures.transformAsync(customerIdFuture, customerId ->
                        changeOwnerAsync(ctx, originator, customerId), MoreExecutors.directExecutor());
            }
            default -> throw new IllegalArgumentException(unsupportedOwnerTypeErrorMessage());
        }
        withCallback(changeOwnerFuture, __ -> ctx.tellSuccess(msg), t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    private ListenableFuture<CustomerId> findOrCreateCustomer(TbContext ctx, TbMsg msg, String ownerName) {
        CustomerService customerService = ctx.getCustomerService();
        TenantId tenantId = ctx.getTenantId();
        ListenableFuture<Optional<Customer>> optionalCustomerListenableFuture = customerService.findCustomerByTenantIdAndTitleAsync(tenantId, ownerName);
        if (config.isCreateOwnerIfNotExists()) {
            return Futures.transform(optionalCustomerListenableFuture,
                    customerOpt -> {
                        if (customerOpt.isPresent()) {
                            return customerOpt.get().getId();
                        }
                        try {
                            Customer newCustomer = new Customer();
                            newCustomer.setTitle(ownerName);
                            newCustomer.setTenantId(tenantId);
                            if (config.isCreateOwnerOnOriginatorLevel()) {
                                EntityId currentOriginatorOwnerId = ctx.getPeContext()
                                        .getOwner(tenantId, msg.getOriginator());
                                newCustomer.setOwnerId(currentOriginatorOwnerId);
                            }
                            Customer savedCustomer = customerService.saveCustomer(newCustomer);
                            ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                                    () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                                    throwable -> log.warn("Failed to push Device Created message: {}", savedCustomer, throwable));
                            return savedCustomer.getId();
                        } catch (DataValidationException e) {
                            customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, ownerName);
                            if (customerOpt.isPresent()) {
                                return customerOpt.get().getId();
                            }
                            throw new RuntimeException("Failed to create customer with title '" + ownerName + "'", e);
                        }
                    }, MoreExecutors.directExecutor());
        }
        return Futures.transform(optionalCustomerListenableFuture, customer -> {
            if (customer.isPresent()) {
                return customer.get().getId();
            }
            throw new NoSuchElementException("Customer with title '" + ownerName + "' doesn't exist!");
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<Void> changeOwnerAsync(TbContext ctx, EntityId originator, EntityId targetOwnerId) {
        TenantId tenantId = ctx.getTenantId();
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityId currentOwnerId = ctx.getPeContext().getOwner(tenantId, originator);
            if (!currentOwnerId.equals(targetOwnerId)) {
                ctx.getPeContext().changeEntityOwner(tenantId, targetOwnerId, originator);
            }
            return null;
        });
    }

    private String unsupportedOwnerTypeErrorMessage() {
        return "Unsupported owner type '" + ownerType +
                "'! Only " + supportedEntityTypesStr + " types are allowed.";
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.has("createOwnerOnOriginatorLevel")) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put("createOwnerOnOriginatorLevel", false)
                            .remove("ownerCacheExpiration");
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
