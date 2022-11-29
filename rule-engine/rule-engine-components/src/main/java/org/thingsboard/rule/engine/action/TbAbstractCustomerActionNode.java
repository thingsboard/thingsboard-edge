/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractCustomerActionNode<C extends TbAbstractCustomerActionNodeConfiguration> implements TbNode {

    protected C config;

    private LoadingCache<CustomerKey, Optional<CustomerId>> customerIdCache;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadCustomerNodeActionConfig(configuration);
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (this.config.getCustomerCacheExpiration() > 0) {
            cacheBuilder.expireAfterWrite(this.config.getCustomerCacheExpiration(), TimeUnit.SECONDS);
        }
        customerIdCache = cacheBuilder
                .build(new CustomerCacheLoader(ctx, createCustomerIfNotExists()));
    }

    protected abstract boolean createCustomerIfNotExists();

    protected abstract C loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processCustomerAction(ctx, msg),
                m -> ctx.tellSuccess(msg),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg) {
        ListenableFuture<CustomerId> customerIdFeature = getCustomer(ctx, msg);
        return Futures.transform(customerIdFeature, customerId -> {
                    doProcessCustomerAction(ctx, msg, customerId);
                    return null;
                }, ctx.getDbCallbackExecutor()
        );
    }

    protected abstract void doProcessCustomerAction(TbContext ctx, TbMsg msg, CustomerId customerId);

    protected ListenableFuture<CustomerId> getCustomer(TbContext ctx, TbMsg msg) {
        String customerTitle = TbNodeUtils.processPattern(this.config.getCustomerNamePattern(), msg);
        CustomerKey key = new CustomerKey(customerTitle);
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            Optional<CustomerId> customerId = customerIdCache.get(key);
            if (!customerId.isPresent()) {
                throw new RuntimeException("No customer found with name '" + key.getCustomerTitle() + "'."
                        + DataConstants.ENTITY_CREATION_ON_EDGE_NOT_SUPPORTED_WARNING);
            }
            return customerId.get();
        });
    }

    @Override
    public void destroy() {
        if (customerIdCache != null) {
            customerIdCache.invalidateAll();
        }
    }

    @Data
    @AllArgsConstructor
    private static class CustomerKey {
        private String customerTitle;
    }

    private static class CustomerCacheLoader extends CacheLoader<CustomerKey, Optional<CustomerId>> {

        private final TbContext ctx;
        private final boolean createIfNotExists;

        private CustomerCacheLoader(TbContext ctx, boolean createIfNotExists) {
            this.ctx = ctx;
            this.createIfNotExists = createIfNotExists;
        }

        @Override
        public Optional<CustomerId> load(CustomerKey key) {
            CustomerService service = ctx.getCustomerService();
            Optional<Customer> customerOptional =
                    service.findCustomerByTenantIdAndTitle(ctx.getTenantId(), key.getCustomerTitle());
            if (customerOptional.isPresent()) {
                return Optional.of(customerOptional.get().getId());
            }
            // TODO: @voba customers are not created on the edge at the moment
            // else if (createIfNotExists) {
            //    Customer newCustomer = new Customer();
            //    newCustomer.setTitle(key.getCustomerTitle());
            //    newCustomer.setTenantId(ctx.getTenantId());
            //    Customer savedCustomer = service.saveCustomer(newCustomer);
            //    ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
            //            () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
            //            throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
            //    return Optional.of(savedCustomer.getId());
            //}
            return Optional.empty();
        }

    }

}
