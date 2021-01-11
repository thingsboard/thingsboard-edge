/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.common.util.DonAsynchron.withCallback;

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
        CacheBuilder cacheBuilder = CacheBuilder.newBuilder();
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
            ownerName = TbNodeUtils.processPattern(config.getOwnerNamePattern(), msg.getMetaData());
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

