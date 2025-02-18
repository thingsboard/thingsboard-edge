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
package org.thingsboard.server.service.apiusage;

import com.google.common.util.concurrent.Futures;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.telemetry.InternalTelemetryService;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultTbApiUsageStateServiceTest {

    @Mock
    private NotificationRuleProcessor notificationRuleProcessor;

    @Mock
    private ApiUsageStateService apiUsageStateService;

    @Mock
    private TenantService tenantService;

    @Mock
    private InternalTelemetryService tsWsService;

    @Mock
    private MailExecutorService mailExecutor;

    @Mock
    private PartitionService partitionService;

    @Mock
    private TbTenantProfileCache tbTenantProfileCache;

    @Spy
    @InjectMocks
    DefaultTbApiUsageStateService service;

    private TenantApiUsageState tenantApiUsageState;
    private Tenant dummyTenant;

    private static final int MAX_ENABLE_VALUE = 5000;
    private static final long VALUE_WARNING = 4500L;
    private static final long VALUE_DISABLE = 5500L;
    private static final int NEW_MAX_ENABLE_VALUE = 4000;
    private static final double NEW_WARN_THRESHOLD_VALUE = 0.9;
    private static final double WARN_THRESHOLD_VALUE = 0.8;

    @BeforeEach
    public void setUp() {
        var tenantId = TenantId.fromUUID(UUID.randomUUID());
        dummyTenant = new Tenant();
        dummyTenant.setId(tenantId);
        dummyTenant.setEmail("test@tenant.com");

        lenient().when(tsWsService.saveTimeseriesInternal(any())).thenReturn(Futures.immediateFuture(0));

        ReflectionTestUtils.setField(service, "tsWsService", tsWsService);

        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxJSExecutions(MAX_ENABLE_VALUE)
                .maxTransportMessages(MAX_ENABLE_VALUE)
                .maxRuleChains(MAX_ENABLE_VALUE)
                .maxTbelExecutions(MAX_ENABLE_VALUE)
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .maxREExecutions(MAX_ENABLE_VALUE)
                .maxEmails(MAX_ENABLE_VALUE)
                .maxSms(MAX_ENABLE_VALUE)
                .maxCreatedAlarms(MAX_ENABLE_VALUE)
                .warnThreshold(WARN_THRESHOLD_VALUE)
                .build();

        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(config);

        TenantProfile dummyTenantProfile = new TenantProfile();
        dummyTenantProfile.setId(new TenantProfileId(UUID.randomUUID()));
        dummyTenantProfile.setProfileData(profileData);

        ApiUsageState dummyUsageState = getApiUsageState(tenantId);

        tenantApiUsageState = new TenantApiUsageState(dummyTenantProfile, dummyUsageState);

        service.myUsageStates.put(tenantId, tenantApiUsageState);
    }

    @NotNull
    private static ApiUsageState getApiUsageState(TenantId tenantId) {
        ApiUsageState dummyUsageState = new ApiUsageState();
        dummyUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setTbelExecState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        dummyUsageState.setTenantId(tenantId);
        dummyUsageState.setEntityId(tenantId);
        dummyUsageState.setVersion(1L);
        return dummyUsageState;
    }

    @Test
    public void givenTenantIdFromEntityStatesMap_whenGetApiUsageState() {
        ApiUsageState tenantUsageState = service.getApiUsageState(dummyTenant.getTenantId());
        assertThat(tenantUsageState, is(tenantApiUsageState.getApiUsageState()));
        verify(service, never()).getOrFetchState(dummyTenant.getTenantId(), dummyTenant.getTenantId());
    }

    @Test
    public void testGetApiUsageState_fromOtherUsageStates() {
        TenantId tenantId = dummyTenant.getTenantId();
        service.myUsageStates.remove(tenantId);
        ApiUsageState otherState = new ApiUsageState();
        otherState.setTenantId(tenantId);
        service.otherUsageStates.put(tenantId, otherState);
        ApiUsageState result = service.getApiUsageState(tenantId);
        assertThat(result, is(otherState));
    }

    @Test
    public void testGetApiUsageState_whenMyPartition() {
        TenantId tenantId = dummyTenant.getTenantId();
        service.myUsageStates.remove(tenantId);
        service.otherUsageStates.remove(tenantId);

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);
        when(tpi.isMyPartition()).thenReturn(true);
        when(partitionService.resolve(any(), eq(dummyTenant.getId()), eq(dummyTenant.getId()))).thenReturn(tpi);

        TenantApiUsageState fetchedState = tenantApiUsageState;
        doReturn(fetchedState).when(service).getOrFetchState(tenantId, tenantId);
        ApiUsageState result = service.getApiUsageState(tenantId);
        assertThat(result, is(fetchedState.getApiUsageState()));
    }

    @Test
    public void testGetApiUsageState_whenNotMyPartition() {
        TenantId tenantId = dummyTenant.getTenantId();
        service.myUsageStates.remove(tenantId);
        service.otherUsageStates.remove(tenantId);

        TopicPartitionInfo tpi = mock(TopicPartitionInfo.class);
        when(tpi.isMyPartition()).thenReturn(false);
        when(partitionService.resolve(any(), eq(tenantId), eq(tenantId))).thenReturn(tpi);

        ApiUsageState foundState = new ApiUsageState();
        foundState.setTenantId(tenantId);
        when(apiUsageStateService.findTenantApiUsageState(tenantId)).thenReturn(foundState);

        ApiUsageState result = service.getApiUsageState(tenantId);
        assertThat(result, is(foundState));

        assertThat(service.otherUsageStates.get(tenantId), is(foundState));
    }

    @Test
    public void testProcess_AllFeaturesTransitionToWarning() {
        TransportProtos.ToUsageStatsServiceMsg.Builder msgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(dummyTenant.getId().getId().getMostSignificantBits())
                .setTenantIdLSB(dummyTenant.getId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            if (key.getApiFeature() != null) {
                msgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                        .setKey(key.name())
                        .setValue(VALUE_WARNING)
                        .build());
            }
        }

        TransportProtos.ToUsageStatsServiceMsg statsMsg = msgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> msg = new TbProtoQueueMsg<>(UUID.randomUUID(), statsMsg);
        TbCallback callback = mock(TbCallback.class);

        when(apiUsageStateService.update(any(ApiUsageState.class))).thenAnswer(invocation -> {
            ApiUsageState state = invocation.getArgument(0);
            state.setVersion(2L);
            return state;
        });

        when(tenantService.findTenantById(dummyTenant.getTenantId())).thenReturn(dummyTenant);

        service.process(msg, callback);

        verify(callback, times(1)).onSuccess();

        for (ApiFeature feature : ApiFeature.values()) {
            if (containsFeature(feature)) {
                assertEquals(ApiUsageStateValue.WARNING, tenantApiUsageState.getFeatureValue(feature),
                        "For feature " + feature + " expected state WARNING");
            }
        }

        assertEquals(ApiUsageStateValue.WARNING, tenantApiUsageState.getFeatureValue(ApiFeature.JS));

        verify(notificationRuleProcessor, atLeastOnce()).process(any());
        verify(mailExecutor, atLeastOnce()).submit((Runnable) any());
    }

    @Test
    public void testProcess_AllFeaturesTransitionToDisabled() {
        TransportProtos.ToUsageStatsServiceMsg.Builder msgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(dummyTenant.getId().getId().getMostSignificantBits())
                .setTenantIdLSB(dummyTenant.getId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            if (key.getApiFeature() != null) {
                msgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                        .setKey(key.name())
                        .setValue(VALUE_DISABLE)
                        .build());
            }
        }
        TransportProtos.ToUsageStatsServiceMsg statsMsg = msgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> msg = new TbProtoQueueMsg<>(UUID.randomUUID(), statsMsg);
        TbCallback callback = mock(TbCallback.class);

        when(apiUsageStateService.update(any(ApiUsageState.class))).thenAnswer(invocation -> {
            ApiUsageState state = invocation.getArgument(0);
            state.setVersion(2L);
            return state;
        });
        when(tenantService.findTenantById(dummyTenant.getTenantId())).thenReturn(dummyTenant);

        service.process(msg, callback);
        verify(callback, times(1)).onSuccess();

        for (ApiFeature feature : ApiFeature.values()) {
            if (containsFeature(feature)) {
                assertEquals(ApiUsageStateValue.DISABLED, tenantApiUsageState.getFeatureValue(feature),
                        "For feature " + feature + " expected state DISABLED");
            }
        }
        verify(notificationRuleProcessor, atLeastOnce()).process(any());
        verify(mailExecutor, atLeastOnce()).submit((Runnable) any());
    }

    @Test
    public void testOnTenantProfileUpdate_updatesStateForMatchingTenant() {
        TenantProfileId currentProfileId = tenantApiUsageState.getTenantProfileId();

        TenantProfileData newProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration newConfig = DefaultTenantProfileConfiguration.builder()
                .maxJSExecutions(NEW_MAX_ENABLE_VALUE)
                .maxTransportMessages(MAX_ENABLE_VALUE)
                .maxRuleChains(MAX_ENABLE_VALUE)
                .maxTbelExecutions(MAX_ENABLE_VALUE)
                .maxDPStorageDays(NEW_MAX_ENABLE_VALUE)
                .maxREExecutions(MAX_ENABLE_VALUE)
                .maxEmails(NEW_MAX_ENABLE_VALUE)
                .maxSms(MAX_ENABLE_VALUE)
                .maxCreatedAlarms(MAX_ENABLE_VALUE)
                .warnThreshold(NEW_WARN_THRESHOLD_VALUE)
                .build();

        newProfileData.setConfiguration(newConfig);

        TenantProfile newProfile = new TenantProfile();
        newProfile.setId(currentProfileId);
        newProfile.setProfileData(newProfileData);

        when(tbTenantProfileCache.get(currentProfileId)).thenReturn(newProfile);

        service.onTenantProfileUpdate(currentProfileId);

        assertEquals(currentProfileId, tenantApiUsageState.getTenantProfileId());
        assertEquals(newProfileData, tenantApiUsageState.getTenantProfileData());
    }

    @Test
    public void testOnTenantUpdate_updatesStateWhenProfileChanged() {
        TenantProfileId oldProfileId = tenantApiUsageState.getTenantProfileId();

        TenantProfileId newProfileId = new TenantProfileId(UUID.randomUUID());
        TenantProfileData newProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration newConfig = DefaultTenantProfileConfiguration.builder()
                .maxJSExecutions(NEW_MAX_ENABLE_VALUE)
                .maxTransportMessages(MAX_ENABLE_VALUE)
                .maxRuleChains(MAX_ENABLE_VALUE)
                .maxTbelExecutions(NEW_MAX_ENABLE_VALUE)
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .maxREExecutions(NEW_MAX_ENABLE_VALUE)
                .maxEmails(MAX_ENABLE_VALUE)
                .maxSms(MAX_ENABLE_VALUE)
                .maxCreatedAlarms(MAX_ENABLE_VALUE)
                .warnThreshold(NEW_WARN_THRESHOLD_VALUE)
                .build();

        newProfileData.setConfiguration(newConfig);

        TenantProfile newProfile = new TenantProfile();
        newProfile.setId(newProfileId);
        newProfile.setProfileData(newProfileData);

        when(tbTenantProfileCache.get(dummyTenant.getTenantId())).thenReturn(newProfile);

        assertNotEquals(oldProfileId, newProfileId);

        service.onTenantUpdate(dummyTenant.getTenantId());

        assertEquals(newProfileId, tenantApiUsageState.getTenantProfileId());
        assertEquals(newProfileData, tenantApiUsageState.getTenantProfileData());
    }

    private boolean containsFeature(ApiFeature feature) {
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            if (key.getApiFeature() != null && key.getApiFeature().equals(feature)) {
                return true;
            }
        }
        return false;
    }

}
