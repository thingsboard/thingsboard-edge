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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbChangeOwnerNodeTest {
    private final TenantId TENANT_ID = new TenantId(UUID.fromString("d369bbbf-4b21-4ee4-aa6a-afe0073c238e"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("48bc5450-a122-498b-9a88-90438e560cbb"));
    private TbChangeOwnerNodeConfiguration config;
    private TbChangeOwnerNode node;
    private ListeningExecutor dbCallbackExecutorMock;
    @Mock
    private TbContext ctxMock;
    @Mock
    private TbPeContext peContextMock;
    @Mock
    private CustomerService customerServiceMock;

    @Before
    public void setUp() throws Exception {
        config = new TbChangeOwnerNodeConfiguration().defaultConfiguration();
        node = new TbChangeOwnerNode();
        dbCallbackExecutorMock = new TestDbCallbackExecutor();
    }

    @Test
    public void givenDeviceOwnerTenant_whenOnMsg_thenChangeDeviceOwnerToCustomer() throws Exception {
        CustomerId customerId = new CustomerId(UUID.fromString("a23cccb2-ea61-4138-ac5b-1b89b13d0cd2"));
        config.setOwnerType("CUSTOMER");
        config.setOwnerNamePattern("${ownerName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutorMock);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitle(any(), any()))
                .thenReturn(Optional.of(new Customer(customerId)));
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID,
                new TbMsgMetaData(Map.of("ownerName", "Test Customer")), TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(eq(msg));
        verify(customerServiceMock).findCustomerByTenantIdAndTitle(eq(TENANT_ID), eq("Test Customer"));
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(eq(TENANT_ID), eq(customerId), eq(DEVICE_ID), eq(EntityType.DEVICE));
    }

    @Test
    public void givenDeviceOwnerCustomer_whenOnMsg_thenChangeDeviceOwnerToTenant() throws Exception {
        config.setOwnerType("TENANT");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutorMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any()))
                .thenReturn(new CustomerId(UUID.fromString("5f59a446-37d2-42b5-8e1e-5ea39363353b")));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(eq(msg));
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(eq(TENANT_ID), eq(TENANT_ID), eq(DEVICE_ID), eq(EntityType.DEVICE));
    }

    @Test
    public void givenDeviceOwnerTenant_whenOnMsg_thenRemainsOwnerTenant() throws Exception {
        config.setOwnerType("TENANT");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutorMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(eq(msg));
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock, times(0)).changeEntityOwner(eq(TENANT_ID), eq(TENANT_ID), eq(DEVICE_ID), eq(EntityType.DEVICE));
    }

    @Test
    public void givenCustomerDoesntExistAndCreateOwnerIfNotExistsIsTrue_whenOnMsg_thenThrowsException() throws Exception {
        CustomerId newCustomerId = new CustomerId(UUID.fromString("04ce4d31-e5f4-4925-a51b-48c6a42aca58"));
        config.setOwnerType("CUSTOMER");
        config.setOwnerNamePattern("${ownerName}");
        config.setCreateOwnerIfNotExists(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutorMock);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(customerServiceMock.findCustomerByTenantIdAndTitle(any(), any())).thenReturn(Optional.empty());
        when(customerServiceMock.saveCustomer(any())).thenReturn(new Customer(newCustomerId));
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID,
                new TbMsgMetaData(Map.of("ownerName", "Test Customer")), TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(eq(msg));
        verify(customerServiceMock).findCustomerByTenantIdAndTitle(eq(TENANT_ID), eq("Test Customer"));
        verify(customerServiceMock).saveCustomer(any(Customer.class));
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(eq(TENANT_ID), eq(newCustomerId), eq(DEVICE_ID), eq(EntityType.DEVICE));
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }
}