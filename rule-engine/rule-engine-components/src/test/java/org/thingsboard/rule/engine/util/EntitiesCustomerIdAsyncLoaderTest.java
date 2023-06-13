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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.user.UserService;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EntitiesCustomerIdAsyncLoaderTest {

    private static final EnumSet<EntityType> SUPPORTED_ENTITY_TYPES = EnumSet.of(
            EntityType.CUSTOMER,
            EntityType.USER,
            EntityType.ASSET,
            EntityType.DEVICE
    );
    private static final ListeningExecutor DB_EXECUTOR = new ListeningExecutor() {
        @Override
        public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
            try {
                return Futures.immediateFuture(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void execute(@NotNull Runnable command) {
            command.run();
        }
    };
    @Mock
    private TbContext ctxMock;
    @Mock
    private UserService userServiceMock;
    @Mock
    private AssetService assetServiceMock;
    @Mock
    private DeviceService deviceServiceMock;

    @Test
    public void givenCustomerEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var customer = new Customer(new CustomerId(UUID.randomUUID()));

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, customer.getId()).get();

        // THEN
        assertEquals(customer.getId(), actualCustomerId);
    }

    @Test
    public void givenUserEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var user = new User(new UserId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        user.setCustomerId(expectedCustomerId);

        when(ctxMock.getUserService()).thenReturn(userServiceMock);
        doReturn(Futures.immediateFuture(user)).when(userServiceMock).findUserByIdAsync(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, user.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    @Test
    public void givenAssetEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var asset = new Asset(new AssetId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        asset.setCustomerId(expectedCustomerId);

        when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
        doReturn(Futures.immediateFuture(asset)).when(assetServiceMock).findAssetByIdAsync(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, asset.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    @Test
    public void givenDeviceEntityType_whenFindEntityIdAsync_thenOK() throws ExecutionException, InterruptedException {
        // GIVEN
        var device = new Device(new DeviceId(UUID.randomUUID()));
        var expectedCustomerId = new CustomerId(UUID.randomUUID());
        device.setCustomerId(expectedCustomerId);

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        doReturn(device).when(deviceServiceMock).findDeviceById(any(), any());
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var actualCustomerId = EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, device.getId()).get();

        // THEN
        assertEquals(expectedCustomerId, actualCustomerId);
    }

    @Test
    public void givenUnsupportedEntityTypes_whenFindEntityIdAsync_thenException() {
        for (var entityType : EntityType.values()) {
            if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
                var entityId = EntityIdFactory.getByTypeAndUuid(entityType, UUID.randomUUID());

                var expectedExceptionMsg = "org.thingsboard.rule.engine.api.TbNodeException: Unexpected originator EntityType: " + entityType;

                var exception = assertThrows(ExecutionException.class,
                        () -> EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctxMock, entityId).get());

                assertInstanceOf(TbNodeException.class, exception.getCause());
                assertEquals(expectedExceptionMsg, exception.getMessage());
            }
        }
    }

}
