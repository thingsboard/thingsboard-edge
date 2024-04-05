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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.customer.CustomerService;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbChangeOwnerNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.TENANT, EntityType.CUSTOMER);
    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(", "));

    private final TenantId TENANT_ID = new TenantId(UUID.fromString("d369bbbf-4b21-4ee4-aa6a-afe0073c238e"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("48bc5450-a122-498b-9a88-90438e560cbb"));
    private final ListeningExecutor dbCallbackExecutor = new TestDbCallbackExecutor();

    private TbChangeOwnerNodeConfiguration config;
    private TbChangeOwnerNode node;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbPeContext peContextMock;
    @Mock
    private CustomerService customerServiceMock;

    @BeforeEach
    public void setUp() {
        config = new TbChangeOwnerNodeConfiguration().defaultConfiguration();
        node = spy(new TbChangeOwnerNode());
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
    public void givenDeviceOwnerCustomerAndCreateOwnerIfNotExistsIsTrueAndCreateOwnerOnOriginatorLevelIsTrue_whenOnMsg_thenChangeOwnerToCustomerOriginatorLevel() throws Exception {
        CustomerId ownerId = new CustomerId(UUID.fromString("07c86754-bd6c-4b56-bd8d-df0e21ffcf9b"));
        CustomerId newCustomerId = new CustomerId(UUID.fromString("04ce4d31-e5f4-4925-a51b-48c6a42aca58"));
        Device device = new Device(DEVICE_ID);
        device.setOwnerId(ownerId);
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
        when(peContextMock.getOwner(any(), any())).thenReturn(ownerId);

        TbMsg msg = TbMsg.newMsg(
                TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID,
                new TbMsgMetaData(Map.of("ownerName", "Test Customer")), TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(eq(msg));
        verify(customerServiceMock, times(2)).findCustomerByTenantIdAndTitle(eq(TENANT_ID), eq("Test Customer"));
        Customer newCustomer = new Customer();
        newCustomer.setTitle("Test Customer");
        newCustomer.setTenantId(TENANT_ID);
        newCustomer.setOwnerId(ownerId);
        verify(customerServiceMock).saveCustomer(eq(newCustomer));
        verify(peContextMock, times(2)).getOwner(TENANT_ID, DEVICE_ID);
        verify(peContextMock).changeEntityOwner(eq(TENANT_ID), eq(newCustomerId), eq(DEVICE_ID));
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    public void givenConfigAnyOwnerType_whenInit_thenVerify(EntityType ownerType) {
        config.setOwnerType(ownerType);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        if (ownerType == null) {
            assertThatThrownBy(() -> node.init(ctxMock, configuration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage("Owner type should be specified!");
            return;
        }
        if (!supportedEntityTypes.contains(ownerType)) {
            assertThatThrownBy(() -> node.init(ctxMock, configuration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage("Unsupported owner type '" + ownerType +
                            "'! Only " + supportedEntityTypesStr + " types are allowed.");
            return;
        }
        if (EntityType.CUSTOMER.equals(ownerType) && StringUtils.isBlank(config.getOwnerNamePattern())) {
            assertThatThrownBy(() -> node.init(ctxMock, configuration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage("Owner name should be specified!");
        }
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 0 with ownerCacheExpiration
                Arguments.of(0,
                        "{\"ownerCacheExpiration\":300,\"createOwnerIfNotExists\":false}",
                        true,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}"
                ),
                // config for version 0
                Arguments.of(0,
                        "{\"createOwnerIfNotExists\":false}",
                        true,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}"
                ),
                // config for version 1
                Arguments.of(1,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}",
                        false,
                        "{\"createOwnerIfNotExists\":false,\"createOwnerOnOriginatorLevel\":false}"
                )
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
