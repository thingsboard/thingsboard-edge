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

import com.google.gson.JsonParser;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

class DeviceState {

    private DeviceProfileState deviceProfile;
    private DeviceDataSnapshot latestValues;
    private final ConcurrentMap<String, DeviceProfileAlarmState> alarmStates = new ConcurrentHashMap<>();

    public DeviceState(DeviceProfileState deviceProfile) {
        this.deviceProfile = deviceProfile;
    }

    public void process(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        if (latestValues == null) {
            latestValues = fetchLatestValues(ctx, msg.getOriginator());
        }
        if (msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name())) {
            processTelemetry(ctx, msg);
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private void processTelemetry(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToSortedTelemetry(new JsonParser().parse(msg.getData()), TbMsgTimeseriesNode.getTs(msg));
        for (Map.Entry<Long, List<KvEntry>> entry : tsKvMap.entrySet()) {
            Long ts = entry.getKey();
            List<KvEntry> data = entry.getValue();
            latestValues = merge(latestValues, ts, data);
            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                DeviceProfileAlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(), a -> new DeviceProfileAlarmState(msg.getOriginator(), alarm));
                alarmState.process(ctx, msg, latestValues);
            }
        }
        ctx.tellSuccess(msg);
    }

    private DeviceDataSnapshot merge(DeviceDataSnapshot latestValues, Long ts, List<KvEntry> data) {
        latestValues.setTs(ts);
        for (KvEntry entry : data) {
            latestValues.putValue(new EntityKey(EntityKeyType.TIME_SERIES, entry.getKey()), toEntityValue(entry));
        }
        return latestValues;
    }

    private DeviceDataSnapshot fetchLatestValues(TbContext ctx, EntityId originator) throws ExecutionException, InterruptedException {
        DeviceDataSnapshot result = new DeviceDataSnapshot(deviceProfile.getEntityKeys());

        Set<String> serverAttributeKeys = new HashSet<>();
        Set<String> clientAttributeKeys = new HashSet<>();
        Set<String> sharedAttributeKeys = new HashSet<>();
        Set<String> commonAttributeKeys = new HashSet<>();
        Set<String> latestTsKeys = new HashSet<>();

        Device device = null;
        for (EntityKey entityKey : deviceProfile.getEntityKeys()) {
            String key = entityKey.getKey();
            switch (entityKey.getType()) {
                case SERVER_ATTRIBUTE:
                    serverAttributeKeys.add(key);
                    break;
                case CLIENT_ATTRIBUTE:
                    clientAttributeKeys.add(key);
                    break;
                case SHARED_ATTRIBUTE:
                    sharedAttributeKeys.add(key);
                    break;
                case ATTRIBUTE:
                    serverAttributeKeys.add(key);
                    clientAttributeKeys.add(key);
                    sharedAttributeKeys.add(key);
                    commonAttributeKeys.add(key);
                    break;
                case TIME_SERIES:
                    latestTsKeys.add(key);
                    break;
                case ENTITY_FIELD:
                    if (device == null) {
                        device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(originator.getId()));
                    }
                    if (device != null) {
                        switch (key) {
                            case EntityKeyMapping.NAME:
                                result.putValue(entityKey, EntityKeyValue.fromString(device.getName()));
                                break;
                            case EntityKeyMapping.TYPE:
                                result.putValue(entityKey, EntityKeyValue.fromString(device.getType()));
                                break;
                            case EntityKeyMapping.CREATED_TIME:
                                result.putValue(entityKey, EntityKeyValue.fromLong(device.getCreatedTime()));
                                break;
                            case EntityKeyMapping.LABEL:
                                result.putValue(entityKey, EntityKeyValue.fromString(device.getLabel()));
                                break;
                        }
                    }
                    break;
            }
        }

        if (!latestTsKeys.isEmpty()) {
            List<TsKvEntry> data = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), originator, latestTsKeys).get();
            for (TsKvEntry entry : data) {
                if (entry.getValue() != null) {
                    result.putValue(new EntityKey(EntityKeyType.TIME_SERIES, entry.getKey()), toEntityValue(entry));
                }
            }
        }
        if (!clientAttributeKeys.isEmpty()) {
            addToSnapshot(result, commonAttributeKeys,
                    ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.CLIENT_SCOPE, clientAttributeKeys).get());
        }
        if (!sharedAttributeKeys.isEmpty()) {
            addToSnapshot(result, commonAttributeKeys,
                    ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SHARED_SCOPE, sharedAttributeKeys).get());
        }
        if (!serverAttributeKeys.isEmpty()) {
            addToSnapshot(result, commonAttributeKeys,
                    ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SERVER_SCOPE, serverAttributeKeys).get());
        }

        return result;
    }

    private void addToSnapshot(DeviceDataSnapshot snapshot, Set<String> commonAttributeKeys, List<AttributeKvEntry> data) {
        for (AttributeKvEntry entry : data) {
            if (entry.getValue() != null) {
                EntityKeyValue value = toEntityValue(entry);
                snapshot.putValue(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, entry.getKey()), value);
                if (commonAttributeKeys.contains(entry.getKey())) {
                    snapshot.putValue(new EntityKey(EntityKeyType.ATTRIBUTE, entry.getKey()), value);
                }
            }
        }
    }

    private EntityKeyValue toEntityValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case STRING:
                return EntityKeyValue.fromString(entry.getStrValue().get());
            case LONG:
                return EntityKeyValue.fromLong(entry.getLongValue().get());
            case DOUBLE:
                return EntityKeyValue.fromDouble(entry.getDoubleValue().get());
            case BOOLEAN:
                return EntityKeyValue.fromBool(entry.getBooleanValue().get());
            case JSON:
                return EntityKeyValue.fromJson(entry.getJsonValue().get());
            default:
                throw new RuntimeException("Can't parse entry: " + entry.getDataType());
        }
    }

}
