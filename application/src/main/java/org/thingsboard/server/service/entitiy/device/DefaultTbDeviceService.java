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
package org.thingsboard.server.service.entitiy.device;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbDeviceService extends AbstractTbEntityService implements TbDeviceService {

    @Override
    public Device save(Device device, String accessToken, EntityGroup entityGroup, SecurityUser user) throws ThingsboardException {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device oldDevice =  device.getId() == null ? null : deviceService.findDeviceById(tenantId, device.getId());
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken));
            autoCommit(user, savedDevice.getId());
            createOrUpdateGroupEntity(tenantId, savedDevice, entityGroup, actionType, user);
            tbClusterService.onDeviceUpdated(savedDevice, oldDevice);
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), device, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials credentials, EntityGroup entityGroup, SecurityUser user) throws ThingsboardException {
        ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials));
            createOrUpdateGroupEntity(tenantId, savedDevice, entityGroup, actionType, user);
            tbClusterService.onDeviceUpdated(savedDevice, device);
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), device, null, actionType, user, e);
            throw handleException(e);
        }
    }

    @Override
    public ListenableFuture<Void> delete(Device device, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            List<EdgeId> relatedEdgeIds = findRelatedEdgeIds(tenantId, deviceId);
            deviceService.deleteDevice(tenantId, deviceId);
            notificationEntityService.notifyDeleteDevice(tenantId, deviceId, device.getCustomerId(), device,
                    relatedEdgeIds, user, deviceId.toString());

            return removeAlarmsByEntityId(tenantId, deviceId);
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    ActionType.DELETED, user, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public DeviceCredentials getDeviceCredentialsByDeviceId(Device device, SecurityUser user) throws ThingsboardException {
        ActionType actionType = ActionType.CREDENTIALS_READ;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId));
            notificationEntityService.notifyEntity(tenantId, deviceId, device, device.getCustomerId(),
                    actionType, user, null, deviceId.toString());
            return deviceCredentials;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    actionType, user, e, deviceId.toString());
            throw handleException(e);
        }
    }

    @Override
    public DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials));
            notificationEntityService.notifyUpdateDeviceCredentials(tenantId, deviceId, device.getCustomerId(), device, result, user);
            return result;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    ActionType.CREDENTIALS_UPDATED, user, e, deviceCredentials);
            throw handleException(e);
        }
    }

    @Override
    public ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, SecurityUser user) throws ThingsboardException {
        try {
            ListenableFuture<ClaimResult> future = claimDevicesService.claimDevice(device, customerId, secretKey);

            return Futures.transform(future, result -> {
                if (result != null && result.getResponse().equals(ClaimResponse.SUCCESS)) {
                    notificationEntityService.notifyEntity(tenantId, device.getId(), result.getDevice(), customerId,
                            ActionType.ASSIGNED_TO_CUSTOMER, user, null, device.getId().toString(), customerId.toString(),
                            customerService.findCustomerById(tenantId, customerId).getName());
                }
                return result;
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, SecurityUser user) throws ThingsboardException {
        try {
            ListenableFuture<ReclaimResult> future = claimDevicesService.reClaimDevice(tenantId, device);

            return Futures.transform(future, result -> {
                Customer unassignedCustomer = result.getUnassignedCustomer();
                if (unassignedCustomer != null) {
                    notificationEntityService.notifyEntity(tenantId, device.getId(), device, device.getCustomerId(), ActionType.UNASSIGNED_FROM_CUSTOMER, user, null,
                            device.getId().toString(), unassignedCustomer.getId().toString(), unassignedCustomer.getName());
                }
                return result;
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Device assignDeviceToTenant(Device device, Tenant newTenant, SecurityUser user) throws ThingsboardException {
        TenantId tenantId = device.getTenantId();
        TenantId newTenantId = newTenant.getId();
        try {
            Tenant tenant = tenantService.findTenantById(tenantId);
            Device assignedDevice = deviceService.assignDeviceToTenant(newTenantId, device);

            notificationEntityService.notifyAssignDeviceToTenant(tenantId, newTenantId, device.getId(),
                    assignedDevice.getCustomerId(), assignedDevice, tenant, user, newTenantId.toString(), newTenant.getName());

            return assignedDevice;
        } catch (Exception e) {
            notificationEntityService.notifyEntity(tenantId, emptyId(EntityType.DEVICE), null, null,
                    ActionType.ASSIGNED_TO_TENANT, user, e, newTenantId.toString());
            throw handleException(e);
        }
    }

}
