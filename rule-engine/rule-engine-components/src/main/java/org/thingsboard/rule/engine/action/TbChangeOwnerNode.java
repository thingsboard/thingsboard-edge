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
package org.thingsboard.rule.engine.action;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "change owner",
        configClazz = TbChangeOwnerNodeConfiguration.class,
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

    private TbChangeOwnerNodeConfiguration config;
    private LoadingCache<OwnerKey, EntityId> ownerIdCache;


    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbChangeOwnerNodeConfiguration.class);
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (config.getOwnerCacheExpiration() > 0) {
            cacheBuilder.expireAfterWrite(this.config.getOwnerCacheExpiration(), TimeUnit.SECONDS);
        }
        ownerIdCache = cacheBuilder
                .build(new OwnerCacheLoader(ctx, config.isCreateOwnerIfNotExists()));
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        processChangeOwner(ctx, msg);
    }

    @Override
    public void destroy() {
        if (ownerIdCache != null) {
            ownerIdCache.invalidateAll();
        }
    }

    private void processChangeOwner(TbContext ctx, TbMsg msg) {
        ListenableFuture<EntityId> ownerIdListenableFuture = getNewOwner(ctx, msg);
        withCallback(ownerIdListenableFuture, ownerId -> {
            try {
                doProcessChangeOwner(ctx, msg, ownerId);
                ctx.tellNext(msg, SUCCESS);
            } catch (ThingsboardException e) {
                ctx.tellFailure(msg, e);
            }
        }, t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<EntityId> getNewOwner(TbContext ctx, TbMsg msg) {
        String ownerName;
        EntityType entityType = EntityType.valueOf(this.config.getOwnerType());
        if(entityType.equals(EntityType.CUSTOMER)) {
            ownerName = TbNodeUtils.processPattern(config.getOwnerNamePattern(), msg);
        } else {
            //Maybe set tenant name?
            ownerName = null;
        }
        OwnerKey key = new OwnerKey(entityType, ownerName);
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityId newOwnerId = ownerIdCache.get(key);
            if (newOwnerId == null) {
                throw new RuntimeException("No owner found with type '" + key.getOwnerType() + "' and name '" + key.getOwnerName() + "'.");
            }
            return newOwnerId;
        });
    }

    private void doProcessChangeOwner(TbContext ctx, TbMsg msg, EntityId ownerId) throws ThingsboardException {
        if (!ctx.getPeContext().getOwner(ctx.getTenantId(), msg.getOriginator()).equals(ownerId)) {
            ctx.getPeContext().changeEntityOwner(ctx.getTenantId(), ownerId, msg.getOriginator(), msg.getOriginator().getEntityType());
        }
    }

    @Data
    @AllArgsConstructor
    private static class OwnerKey {
        private EntityType ownerType;
        private String ownerName;
    }

    private static class OwnerCacheLoader extends CacheLoader<OwnerKey, EntityId> {

        private final TbContext ctx;
        private final boolean createOwnerIfNotExists;

        private OwnerCacheLoader(TbContext ctx, boolean createOwnerIfNotExists) {
            this.ctx = ctx;
            this.createOwnerIfNotExists = createOwnerIfNotExists;
        }

        @Override
        public EntityId load(OwnerKey ownerKey) {
            if (ownerKey.getOwnerType().equals(EntityType.CUSTOMER)) {
                Customer customer;
                CustomerService customerService = ctx.getCustomerService();
                Optional<Customer> customerOptional = customerService.findCustomerByTenantIdAndTitle(ctx.getTenantId(), ownerKey.getOwnerName());
                if (customerOptional.isPresent()) {
                    customer = customerOptional.get();
                    return customer.getId();
                } else if (createOwnerIfNotExists) {
                    Customer newCustomer = new Customer();
                    newCustomer.setTitle(ownerKey.getOwnerName());
                    newCustomer.setTenantId(ctx.getTenantId());
                    customer = customerService.saveCustomer(newCustomer);
                    return customer.getId();
                } else {
                    return null;
                }
            } else {
                return ctx.getTenantId();
            }
        }
    }
}

