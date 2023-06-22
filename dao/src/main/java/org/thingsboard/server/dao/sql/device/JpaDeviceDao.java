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
package org.thingsboard.server.dao.sql.device;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
@Slf4j
public class JpaDeviceDao extends JpaAbstractDao<DeviceEntity, Device> implements DeviceDao {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private NativeDeviceRepository nativeDeviceRepository;

    @Override
    protected Class<DeviceEntity> getEntityClass() {
        return DeviceEntity.class;
    }

    @Override
    protected JpaRepository<DeviceEntity, UUID> getRepository() {
        return deviceRepository;
    }

    @Override
    @Transactional
    public Device saveAndFlush(TenantId tenantId, Device device) {
        Device result = this.save(tenantId, device);
        deviceRepository.flush();
        return result;
    }

    @Override
    public PageData<Device> findDevicesByTenantId(UUID tenantId, PageLink pageLink) {
        if (StringUtils.isEmpty(pageLink.getTextSearch())) {
            return DaoUtil.toPageData(
                    deviceRepository.findByTenantId(
                            tenantId,
                            DaoUtil.toPageable(pageLink)));
        } else {
            return DaoUtil.toPageData(
                    deviceRepository.findByTenantId(
                            tenantId,
                            Objects.toString(pageLink.getTextSearch(), ""),
                            DaoUtil.toPageable(pageLink)));
        }
    }

    @Override
    public Long countDevices() {
        return deviceRepository.count();
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(deviceIds, ids ->
                deviceRepository.findDevicesByTenantIdAndIdIn(tenantId, ids), service);
    }

    @Override
    public PageData<Device> findDevicesByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(deviceRepository
                .findByEntityGroupId(
                        groupId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Device> findDevicesByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(deviceRepository
                .findByEntityGroupIds(
                        groupIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Device> findDevicesByEntityGroupIdsAndType(List<UUID> groupIds, String type, PageLink pageLink) {
        return DaoUtil.toPageData(deviceRepository
                .findByEntityGroupIdsAndType(
                        groupIds,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<Device> findDevicesByIds(List<UUID> deviceIds) {
        return DaoUtil.convertDataList(deviceRepository.findDevicesByIdIn(deviceIds));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByIdsAsync(List<UUID> deviceIds) {
        return service.submit(() -> findDevicesByIds(deviceIds));
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndProfileId(UUID tenantId, UUID profileId, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndProfileId(
                        tenantId,
                        profileId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink) {
        return DaoUtil.pageToPageData(deviceRepository.findIdsByDeviceProfileTransportType(transportType, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        return DaoUtil.getEntitiesByTenantIdAndIdIn(deviceIds, ids ->
                deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, ids), service);
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(device);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink) {
        return DaoUtil.toPageData(
                deviceRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<DeviceId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if(customerId == null){
            page = deviceRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = deviceRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, DeviceId::new);
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantDeviceTypesToDto(tenantId, deviceRepository.findTenantDeviceTypes(tenantId)));
    }

    @Override
    public Device findDeviceByTenantIdAndId(TenantId tenantId, UUID id) {
        return DaoUtil.getData(deviceRepository.findByTenantIdAndId(tenantId.getId(), id));
    }

    @Override
    public ListenableFuture<Device> findDeviceByTenantIdAndIdAsync(TenantId tenantId, UUID id) {
        return service.submit(() -> DaoUtil.getData(deviceRepository.findByTenantIdAndId(tenantId.getId(), id)));
    }

    @Override
    public Long countDevicesByDeviceProfileId(TenantId tenantId, UUID deviceProfileId) {
        return deviceRepository.countByDeviceProfileId(deviceProfileId);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return deviceRepository.countByTenantId(tenantId.getId());
    }

    private List<EntitySubtype> convertTenantDeviceTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(TenantId.fromUUID(tenantId), EntityType.DEVICE, type));
            }
        }
        return list;
    }

    @Override
    public PageData<Device> findByEntityGroupAndDeviceProfileAndEmptyOtaPackage(UUID groupId,
                                                                                UUID deviceProfileId,
                                                                                OtaPackageType type,
                                                                                PageLink pageLink) {
        Page<DeviceEntity> page = OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.findByEntityGroupIdAndDeviceProfileIdAndFirmwareIdIsNull(
                        groupId, deviceProfileId, Objects.toString(pageLink.getTextSearch(), ""), DaoUtil.toPageable(pageLink)),
                () -> deviceRepository.findByEntityGroupIdAndDeviceProfileIdAndSoftwareIdIsNull(
                        groupId, deviceProfileId, Objects.toString(pageLink.getTextSearch(), ""), DaoUtil.toPageable(pageLink)),
                type);
        return DaoUtil.toPageData(page);
    }

    @Override
    public PageData<Device> findByDeviceProfileAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId,
                                                                  OtaPackageType type,
                                                                  PageLink pageLink) {
        Page<DeviceEntity> page = OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.findByDeviceProfileIdAndFirmwareIdIsNull(tenantId, deviceProfileId, DaoUtil.toPageable(pageLink)),
                () -> deviceRepository.findByDeviceProfileIdAndSoftwareIdIsNull(tenantId, deviceProfileId, DaoUtil.toPageable(pageLink)),
                type);
        return DaoUtil.toPageData(page);
    }

    @Override
    public Long countByEntityGroupAndEmptyOtaPackage(UUID groupId, UUID otaPackageId, OtaPackageType type) {
        return OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.countByEntityGroupIdAndFirmwareIdIsNull(groupId, otaPackageId),
                () -> deviceRepository.countByEntityGroupIdAndSoftwareIdIsNull(groupId, otaPackageId),
                type);
    }

    @Override
    public Long countByDeviceProfileAndEmptyOtaPackage(UUID tenantId, UUID deviceProfileId, OtaPackageType type) {
        return OtaPackageUtil.getByOtaPackageType(
                () -> deviceRepository.countByDeviceProfileIdAndFirmwareIdIsNull(tenantId, deviceProfileId),
                () -> deviceRepository.countByDeviceProfileIdAndSoftwareIdIsNull(tenantId, deviceProfileId),
                type);
    }

    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink) {
        log.debug("Try to find tenant device id infos by pageLink [{}]", pageLink);
        return nativeDeviceRepository.findDeviceIdInfos(DaoUtil.toPageable(pageLink));
    }

    @Override
    public Device findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(deviceRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Device findByTenantIdAndName(UUID tenantId, String name) {
        return findDeviceByTenantIdAndName(tenantId, name).orElse(null);
    }

    @Override
    public PageData<Device> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findDevicesByTenantId(tenantId, pageLink);
    }

    @Override
    public DeviceId getExternalIdByInternal(DeviceId internalId) {
        return Optional.ofNullable(deviceRepository.getExternalIdById(internalId.getId()))
                .map(DeviceId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

}
