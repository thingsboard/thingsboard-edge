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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
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
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbChangeOwnerNodeTest extends AbstractRuleNodeUpgradeTest {

    private final TenantId TENANT_ID = new TenantId(UUID.fromString("d369bbbf-4b21-4ee4-aa6a-afe0073c238e"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("48bc5450-a122-498b-9a88-90438e560cbb"));

    private TbChangeOwnerNodeConfiguration config;
    private TbChangeOwnerNode node;
    private ListeningExecutor dbCallbackExecutor;

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
        dbCallbackExecutor = new TestDbCallbackExecutor();
    }

    @Test
    public void givenDeviceOwnerTenant_whenOnMsg_thenChangeDeviceOwnerToCustomer() throws Exception {
        CustomerId customerId = new CustomerId(UUID.fromString("a23cccb2-ea61-4138-ac5b-1b89b13d0cd2"));
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("${ownerName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
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
        verify(peContextMock).changeEntityOwner(eq(TENANT_ID), eq(customerId), eq(DEVICE_ID));
    }

    @Test
    public void givenDeviceOwnerCustomer_whenOnMsg_thenChangeDeviceOwnerToTenant() throws Exception {
        config.setOwnerType(EntityType.TENANT);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any()))
                .thenReturn(new CustomerId(UUID.fromString("5f59a446-37d2-42b5-8e1e-5ea39363353b")));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(eq(msg));
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(eq(TENANT_ID), eq(TENANT_ID), eq(DEVICE_ID));
    }

    @Test
    public void givenDeviceOwnerTenant_whenOnMsg_thenRemainsOwnerTenant() throws Exception {
        config.setOwnerType(EntityType.TENANT);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(eq(msg));
        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock, never()).changeEntityOwner(eq(TENANT_ID), eq(TENANT_ID), eq(DEVICE_ID));
    }

    @Test
    public void givenDeviceOwnerTenantAndCreateOwnerIfNotExistsIsTrueAndCreateOwnerOnOriginatorLevelIsTrue_whenOnMsg_thenChangeOwnerToCustomerOriginatorLevel() throws Exception {
        CustomerId newCustomerId = new CustomerId(UUID.fromString("04ce4d31-e5f4-4925-a51b-48c6a42aca58"));
        Device device = new Device(DEVICE_ID);
        device.setOwnerId(TENANT_ID);
        config.setOwnerType(EntityType.CUSTOMER);
        config.setOwnerNamePattern("${ownerName}");
        config.setCreateOwnerIfNotExists(true);
        config.setCreateOwnerOnOriginatorLevel(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
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
        verify(customerServiceMock, times(2)).findCustomerByTenantIdAndTitle(eq(TENANT_ID), eq("Test Customer"));
        Customer newCustomer = new Customer();
        newCustomer.setTitle("Test Customer");
        newCustomer.setTenantId(TENANT_ID);
        newCustomer.setOwnerId(TENANT_ID);
        verify(customerServiceMock).saveCustomer(eq(newCustomer));
        verify(peContextMock, times(2)).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(eq(TENANT_ID), eq(newCustomerId), eq(DEVICE_ID));
    }

    @Test
    public void givenDefaultConfig_whenInit_thenThrowsException() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Owner type should be specified!");
    }

    @Test
    public void givenConfigWithUnsupportedType_whenInit_thenThrowsException() {
        config.setOwnerType(EntityType.DEVICE);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatThrownBy(() -> node.init(ctxMock, configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Unsupported owner type 'DEVICE'! Only TENANT, CUSTOMER types are allowed.");
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                Arguments.of(0,
                        "{\"ownerCacheExpiration\":300,\"createOwnerIfNotExists\":false}",
                        true,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return spy(new TbChangeOwnerNode());
    }
}
