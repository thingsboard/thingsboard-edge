/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbDeviceProfileNodeTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbDeviceProfileNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private RuleEngineDeviceProfileCache cache;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private RuleEngineAlarmService alarmService;

    private TenantId tenantId = new TenantId(UUID.randomUUID());
    private DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());

    @Test
    public void testRandomMessageType() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg("123456789", deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testEmptyProfile() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testAlarmCreate() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        KeyFilter highTempFilter = new KeyFilter();
        highTempFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule));

        KeyFilter lowTempFilter = new KeyFilter();
        lowTempFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRule clearRule = new AlarmRule();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
        clearRule.setCondition(clearCondition);
        dpa.setClearRule(clearRule);

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm")).thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

        TbMsg theMsg2 = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "2");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg2);


        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).tellNext(theMsg2, "Alarm Updated");

    }

    private void init() throws TbNodeException {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getDeviceProfileCache()).thenReturn(cache);
        Mockito.when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        Mockito.when(ctx.getAlarmService()).thenReturn(alarmService);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.createObjectNode());
        node = new TbDeviceProfileNode();
        node.init(ctx, nodeConfiguration);
    }

}
