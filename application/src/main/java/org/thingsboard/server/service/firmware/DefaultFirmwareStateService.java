/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.firmware;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.firmware.DeviceGroupFirmware;
import org.thingsboard.server.common.data.firmware.FirmwareInfo;
import org.thingsboard.server.common.data.firmware.FirmwareType;
import org.thingsboard.server.common.data.firmware.FirmwareUpdateStatus;
import org.thingsboard.server.common.data.firmware.FirmwareUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.FirmwareId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.firmware.FirmwareService;
import org.thingsboard.server.gen.transport.TransportProtos.ToFirmwareStateServiceMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.common.data.firmware.FirmwareKey.CHECKSUM;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.CHECKSUM_ALGORITHM;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.ID;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.SIZE;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.STATE;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.TITLE;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.TS;
import static org.thingsboard.server.common.data.firmware.FirmwareKey.VERSION;
import static org.thingsboard.server.common.data.firmware.FirmwareType.FIRMWARE;
import static org.thingsboard.server.common.data.firmware.FirmwareType.SOFTWARE;
import static org.thingsboard.server.common.data.firmware.FirmwareUtil.getAttributeKey;
import static org.thingsboard.server.common.data.firmware.FirmwareUtil.getTargetTelemetryKey;
import static org.thingsboard.server.common.data.firmware.FirmwareUtil.getTelemetryKey;

@Slf4j
@Service
@TbCoreComponent
public class DefaultFirmwareStateService implements FirmwareStateService {

    private final TbClusterService tbClusterService;
    private final FirmwareService firmwareService;
    private final DeviceService deviceService;
    private final RuleEngineTelemetryService telemetryService;
    private final AttributesService attributesService;
    private final DbCallbackExecutorService dbExecutor;
    private final TbQueueProducer<TbProtoQueueMsg<ToFirmwareStateServiceMsg>> fwStateMsgProducer;

    public DefaultFirmwareStateService(TbClusterService tbClusterService, FirmwareService firmwareService,
                                       DeviceService deviceService,
                                       RuleEngineTelemetryService telemetryService,
                                       AttributesService attributesService, DbCallbackExecutorService dbExecutor, TbCoreQueueFactory coreQueueFactory) {
        this.tbClusterService = tbClusterService;
        this.firmwareService = firmwareService;
        this.deviceService = deviceService;
        this.telemetryService = telemetryService;
        this.attributesService = attributesService;
        this.dbExecutor = dbExecutor;
        this.fwStateMsgProducer = coreQueueFactory.createToFirmwareStateServiceMsgProducer();
    }

    @Override
    public void update(TenantId tenantId, DeviceGroupFirmware newDeviceGroupFirmware, DeviceGroupFirmware oldDeviceGroupFirmware) {
        long ts = System.currentTimeMillis();

        if (oldDeviceGroupFirmware == null) {
            FirmwareInfo newFirmware = firmwareService.findFirmwareInfoById(tenantId, newDeviceGroupFirmware.getFirmwareId());
            update(newDeviceGroupFirmware, newFirmware, ts);
        } else if (newDeviceGroupFirmware == null) {
            FirmwareInfo oldFirmware = firmwareService.findFirmwareInfoById(tenantId, oldDeviceGroupFirmware.getFirmwareId());
            remove(oldDeviceGroupFirmware, oldFirmware, ts);
        } else {
            FirmwareInfo newFirmware = firmwareService.findFirmwareInfoById(tenantId, newDeviceGroupFirmware.getFirmwareId());
            FirmwareInfo oldFirmware = firmwareService.findFirmwareInfoById(tenantId, oldDeviceGroupFirmware.getFirmwareId());
            update(newDeviceGroupFirmware, newFirmware, ts);
            if (!newFirmware.getDeviceProfileId().equals(oldFirmware.getDeviceProfileId())) {
                remove(oldDeviceGroupFirmware, oldFirmware, ts);
            }
        }
    }

    private void update(DeviceGroupFirmware deviceGroupFirmware, FirmwareInfo firmwareFromGroup, long ts) {
        PageLink pageLink = createPageLink();
        PageData<Device> pageData;
        do {
            pageData = deviceService.findByEntityGroupAndDeviceProfileAndEmptyFirmware(deviceGroupFirmware.getGroupId(),
                    firmwareFromGroup.getDeviceProfileId(), deviceGroupFirmware.getFirmwareType(), pageLink);
            pageData.getData().forEach(d ->
                    send(d.getTenantId(), d.getId(), deviceGroupFirmware.getFirmwareId(), ts, deviceGroupFirmware.getFirmwareType()));

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    private void remove(DeviceGroupFirmware deviceGroupFirmware, FirmwareInfo firmwareFromGroup, long ts) {
        FirmwareType firmwareType = deviceGroupFirmware.getFirmwareType();
        PageLink pageLink = createPageLink();
        PageData<Device> pageData;
        do {
            pageData = deviceService.findByEntityGroupAndDeviceProfileAndEmptyFirmware(deviceGroupFirmware.getGroupId(),
                    firmwareFromGroup.getDeviceProfileId(), firmwareType, pageLink);
            pageData.getData().forEach(device -> {
                FirmwareInfo firmwareForDevice = firmwareService.findFirmwareInfoByDeviceIdAndFirmwareType(device.getId(), deviceGroupFirmware.getFirmwareType());
                if (firmwareForDevice != null) {
                    ListenableFuture<Optional<AttributeKvEntry>> oldFirmwareIdFuture =
                            attributesService.find(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE, getAttributeKey(firmwareType, ID));
                    DonAsynchron.withCallback(oldFirmwareIdFuture, oldIdOpt -> {
                        if (oldIdOpt.isPresent()) {
                            FirmwareId oldFirmwareId = new FirmwareId(UUID.fromString(oldIdOpt.get().getValueAsString()));
                            if (!firmwareForDevice.getId().equals(oldFirmwareId)) {
                                send(device.getTenantId(), device.getId(), firmwareForDevice.getId(), ts, firmwareType);
                            }
                        } else {
                            log.trace("[{}] Firmware id attribute not found!", device.getId());
                        }
                    }, (e) -> log.error("Failed to get firmware id attribute for device!", e), dbExecutor);
                } else {
                    remove(device, firmwareType);
                }
            });

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    @Override
    public void update(TenantId tenantId, List<DeviceId> deviceIds, boolean isFirmware, boolean isSoftware) {
        deviceIds.forEach(id -> {
            if (isFirmware) {
                update(tenantId, id, FIRMWARE);
            }
            if (isSoftware) {
                update(tenantId, id, SOFTWARE);
            }
        });
    }

    private void update(TenantId tenantId, DeviceId deviceId, FirmwareType firmwareType) {
        FirmwareInfo firmware = firmwareService.findFirmwareInfoByDeviceIdAndFirmwareType(deviceId, firmwareType);
        if (firmware != null) {
            ListenableFuture<Optional<AttributeKvEntry>> oldFirmwareIdFuture =
                    attributesService.find(tenantId, deviceId, DataConstants.SERVER_SCOPE, getAttributeKey(firmwareType, ID));
            DonAsynchron.withCallback(oldFirmwareIdFuture, oldIdOpt -> {
                if (oldIdOpt.isPresent()) {
                    FirmwareId oldFirmwareId = new FirmwareId(UUID.fromString(oldIdOpt.get().getValueAsString()));
                    if (!firmware.getId().equals(oldFirmwareId)) {
                        send(tenantId, deviceId, firmware.getId(), System.currentTimeMillis(), firmwareType);
                    }
                } else {
                    send(tenantId, deviceId, firmware.getId(), System.currentTimeMillis(), firmwareType);
                }
            }, (e) -> log.error("Failed to get firmware id attribute for device!", e), dbExecutor);
        } else {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            remove(device, firmwareType);
        }
    }

    @Override
    public void update(Device device) {
        updateFirmware(device);
        updateSoftware(device);
    }

    private void updateFirmware(Device device) {
        ListenableFuture<Optional<AttributeKvEntry>> oldFirmwareIdFuture = attributesService.find(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE, getAttributeKey(FIRMWARE, ID));
        DonAsynchron.withCallback(oldFirmwareIdFuture, oldIdOpt -> {

            FirmwareId oldFirmwareId = null;

            if (oldIdOpt.isPresent()) {
                oldFirmwareId = new FirmwareId(UUID.fromString(oldIdOpt.get().getValueAsString()));
            }

            FirmwareInfo fw = firmwareService.findFirmwareInfoByDeviceIdAndFirmwareType(device.getId(), FIRMWARE);

            if (fw == null) {
                if (oldFirmwareId != null) {
                    remove(device, FIRMWARE);
                }
            } else if (!fw.getId().equals(oldFirmwareId)) {
                send(device.getTenantId(), device.getId(), fw.getId(), System.currentTimeMillis(), FIRMWARE);
            }

        }, (e) -> log.error("Failed to get firmware id attribute!", e), dbExecutor);
    }

    private void updateSoftware(Device device) {
        ListenableFuture<Optional<AttributeKvEntry>> oldSoftwareIdFuture = attributesService.find(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE, getAttributeKey(SOFTWARE, ID));
        DonAsynchron.withCallback(oldSoftwareIdFuture, oldIdOpt -> {

            FirmwareId oldSoftwareId = null;

            if (oldIdOpt.isPresent()) {
                oldSoftwareId = new FirmwareId(UUID.fromString(oldIdOpt.get().getValueAsString()));
            }

            FirmwareInfo sw = firmwareService.findFirmwareInfoByDeviceIdAndFirmwareType(device.getId(), SOFTWARE);

            if (sw == null) {
                if (oldSoftwareId != null) {
                    remove(device, SOFTWARE);
                }
            } else if (!sw.getId().equals(oldSoftwareId)) {
                send(device.getTenantId(), device.getId(), sw.getId(), System.currentTimeMillis(), SOFTWARE);
            }

        }, (e) -> log.error("Failed to get software id attribute!", e), dbExecutor);
    }

    @Override
    public void update(DeviceProfile deviceProfile, boolean isFirmwareChanged, boolean isSoftwareChanged) {
        TenantId tenantId = deviceProfile.getTenantId();

        if (isFirmwareChanged) {
            update(tenantId, deviceProfile, FIRMWARE);
        }
        if (isSoftwareChanged) {
            update(tenantId, deviceProfile, SOFTWARE);
        }
    }

    private void update(TenantId tenantId, DeviceProfile deviceProfile, FirmwareType firmwareType) {
        Consumer<Device> updateConsumer;

        FirmwareId firmwareId = FirmwareUtil.getFirmwareId(deviceProfile, firmwareType);

        if (firmwareId != null) {
            long ts = System.currentTimeMillis();
            updateConsumer = d -> send(d.getTenantId(), d.getId(), firmwareId, ts, firmwareType);
        } else {
            updateConsumer = d -> remove(d, firmwareType);
        }

        PageLink pageLink = createPageLink();
        PageData<Device> pageData;
        do {
            pageData = deviceService.findByDeviceProfileAndEmptyFirmware(tenantId, deviceProfile.getId(), firmwareType, pageLink);
            pageData.getData().forEach(updateConsumer);

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    @Override
    public boolean process(ToFirmwareStateServiceMsg msg) {
        boolean isSuccess = false;
        FirmwareId targetFirmwareId = new FirmwareId(new UUID(msg.getFirmwareIdMSB(), msg.getFirmwareIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
        TenantId tenantId = new TenantId(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        FirmwareType firmwareType = FirmwareType.valueOf(msg.getType());
        long ts = msg.getTs();

        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device == null) {
            log.warn("[{}] [{}] Device was removed during firmware update msg was queued!", tenantId, deviceId);
        } else {
            FirmwareInfo currentFirmware = firmwareService.findFirmwareInfoByDeviceIdAndFirmwareType(deviceId, firmwareType);

            if (currentFirmware != null && targetFirmwareId.equals(currentFirmware.getId())) {
                update(device, currentFirmware, ts);
                isSuccess = true;
            } else {
                log.warn("[{}] [{}] Can`t update firmware for the device, target firmwareId: [{}], current firmware: [{}]!", tenantId, deviceId, targetFirmwareId, currentFirmware);
            }
        }
        return isSuccess;
    }

    private void send(TenantId tenantId, DeviceId deviceId, FirmwareId firmwareId, long ts, FirmwareType firmwareType) {
        ToFirmwareStateServiceMsg msg = ToFirmwareStateServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setFirmwareIdMSB(firmwareId.getId().getMostSignificantBits())
                .setFirmwareIdLSB(firmwareId.getId().getLeastSignificantBits())
                .setType(firmwareType.name())
                .setTs(ts)
                .build();

        FirmwareInfo firmware = firmwareService.findFirmwareInfoById(tenantId, firmwareId);
        if (firmware == null) {
            log.warn("[{}] Failed to send firmware update because firmware was already deleted", firmwareId);
            return;
        }

        TopicPartitionInfo tpi = new TopicPartitionInfo(fwStateMsgProducer.getDefaultTopic(), null, null, false);
        fwStateMsgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);

        List<TsKvEntry> telemetry = new ArrayList<>();
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTargetTelemetryKey(firmware.getType(), TITLE), firmware.getTitle())));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTargetTelemetryKey(firmware.getType(), VERSION), firmware.getVersion())));
        telemetry.add(new BasicTsKvEntry(ts, new LongDataEntry(getTargetTelemetryKey(firmware.getType(), TS), ts)));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTelemetryKey(firmware.getType(), STATE), FirmwareUpdateStatus.QUEUED.name())));

        telemetryService.saveAndNotify(tenantId, deviceId, telemetry, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save firmware status!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save firmware status!", deviceId, t);
            }
        });

        List<AttributeKvEntry> attributes = new ArrayList<>();
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), ID), firmware.getId().toString())));

        telemetryService.saveAndNotify(tenantId, deviceId, DataConstants.SERVER_SCOPE, attributes, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save attributes with target firmware!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save attributes with target firmware!", deviceId, t);
            }
        });
    }

    private void update(Device device, FirmwareInfo firmware, long ts) {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();

        BasicTsKvEntry status = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(getTelemetryKey(firmware.getType(), STATE), FirmwareUpdateStatus.INITIATED.name()));

        telemetryService.saveAndNotify(tenantId, deviceId, Collections.singletonList(status), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save telemetry with target firmware for device!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save telemetry with target firmware for device!", deviceId, t);
            }
        });

        List<AttributeKvEntry> attributes = new ArrayList<>();
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), TITLE), firmware.getTitle())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), VERSION), firmware.getVersion())));
        attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(getAttributeKey(firmware.getType(), SIZE), firmware.getDataSize())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), CHECKSUM_ALGORITHM), firmware.getChecksumAlgorithm().name())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(firmware.getType(), CHECKSUM), firmware.getChecksum())));

        telemetryService.saveAndNotify(tenantId, deviceId, DataConstants.SHARED_SCOPE, attributes, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save attributes with target firmware!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save attributes with target firmware!", deviceId, t);
            }
        });
    }

    private void remove(Device device, FirmwareType firmwareType) {
        telemetryService.deleteAndNotify(device.getTenantId(), device.getId(), DataConstants.SHARED_SCOPE, FirmwareUtil.getAttributeKeys(firmwareType),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        log.trace("[{}] Success remove target firmware attributes!", device.getId());
                        Set<AttributeKey> keysToNotify = new HashSet<>();
                        FirmwareUtil.ALL_FW_ATTRIBUTE_KEYS.forEach(key -> keysToNotify.add(new AttributeKey(DataConstants.SHARED_SCOPE, key)));
                        tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(device.getTenantId(), device.getId(), keysToNotify), null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to remove target firmware attributes!", device.getId(), t);
                    }
                });

        String idKey = FirmwareUtil.getAttributeKey(firmwareType, ID);

        telemetryService.deleteAndNotify(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(idKey),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        log.trace("[{}] Success remove firmware id attribute!", device.getId());
                        Set<AttributeKey> keysToNotify = new HashSet<>();
                        keysToNotify.add(new AttributeKey(DataConstants.SERVER_SCOPE, idKey));
                        tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(device.getTenantId(), device.getId(), keysToNotify), null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to remove firmware id attribute!!", device.getId(), t);
                    }
                });
    }

    private PageLink createPageLink() {
        return new PageLink(100);
    }
}
