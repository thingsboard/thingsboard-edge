/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.claim.data.ClaimRequest;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DeviceController extends BaseController {

    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_NAME = "deviceName";

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public Device getDeviceById(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            return checkDeviceId(deviceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDevice(@RequestBody Device device,
                             @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId) throws ThingsboardException {
        try {
            device.setTenantId(getCurrentUser().getTenantId());

            Operation operation = device.getId() == null ? Operation.CREATE : Operation.WRITE;

            if (operation == Operation.CREATE
                    && getCurrentUser().getAuthority() == Authority.CUSTOMER_USER &&
                    (device.getCustomerId() == null || device.getCustomerId().isNullUid())) {
                device.setCustomerId(getCurrentUser().getCustomerId());
            }

            EntityGroupId entityGroupId = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
            }

            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, operation,
                    device.getId(), device, entityGroupId);

            Device savedDevice = checkNotNull(deviceService.saveDevice(device));

            if (entityGroupId != null && operation == Operation.CREATE) {
                entityGroupService.addEntityToEntityGroup(getTenantId(), entityGroupId, savedDevice.getId());
            }

            actorService
                    .onDeviceNameOrTypeUpdate(
                            savedDevice.getTenantId(),
                            savedDevice.getId(),
                            savedDevice.getName(),
                            savedDevice.getType());

            logEntityAction(savedDevice.getId(), savedDevice,
                    savedDevice.getCustomerId(),
                    device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            if (device.getId() == null) {
                deviceStateService.onDeviceAdded(savedDevice);
            } else {
                deviceStateService.onDeviceUpdated(savedDevice);
            }
            return savedDevice;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), device,
                    null, device.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDevice(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.DELETE);
            deviceService.deleteDevice(getCurrentUser().getTenantId(), deviceId);

            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.DELETED, null, strDeviceId);

            deviceStateService.onDeviceDeleted(device);
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE),
                    null,
                    null,
                    ActionType.DELETED, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}/credentials", method = RequestMethod.GET)
    @ResponseBody
    public DeviceCredentials getDeviceCredentialsByDeviceId(@PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        try {
            DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
            Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(getCurrentUser().getTenantId(), deviceId));
            logEntityAction(deviceId, device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, null, strDeviceId);
            return deviceCredentials;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_READ, e, strDeviceId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/credentials", method = RequestMethod.POST)
    @ResponseBody
    public DeviceCredentials saveDeviceCredentials(@RequestBody DeviceCredentials deviceCredentials) throws ThingsboardException {
        checkNotNull(deviceCredentials);
        try {
            Device device = checkDeviceId(deviceCredentials.getDeviceId(), Operation.WRITE_CREDENTIALS);
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(getCurrentUser().getTenantId(), deviceCredentials));
            actorService.onCredentialsUpdate(getCurrentUser().getTenantId(), deviceCredentials.getDeviceId());
            logEntityAction(device.getId(), device,
                    device.getCustomerId(),
                    ActionType.CREDENTIALS_UPDATED, null, deviceCredentials);
            return result;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.DEVICE), null,
                    null,
                    ActionType.CREDENTIALS_UPDATED, e, deviceCredentials);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getTenantDevices(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"deviceName"}, method = RequestMethod.GET)
    @ResponseBody
    public Device getTenantDevice(
            @RequestParam String deviceName) throws ThingsboardException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getCustomerDevices(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ);
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/devices", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<Device> getUserDevices(
            @RequestParam int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            List<Predicate<Device>> filters = new ArrayList<>();
            if (type != null && type.trim().length() > 0) {
                filters.add((device -> device.getType().equals(type)));
            }
            return getGroupEntitiesByPageLink(getCurrentUser(), EntityType.DEVICE, Operation.READ, entityId -> new DeviceId(entityId.getId()),
                    (entityIds) -> {
                        try {
                            return deviceService.findDevicesByTenantIdAndIdsAsync(getTenantId(), entityIds).get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    filters,
                    pageLink);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", params = {"deviceIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Device> getDevicesByIds(
            @RequestParam("deviceIds") String[] strDeviceIds) throws ThingsboardException {
        checkArrayParameter("deviceIds", strDeviceIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<DeviceId> deviceIds = new ArrayList<>();
            for (String strDeviceId : strDeviceIds) {
                deviceIds.add(new DeviceId(toUUID(strDeviceId)));
            }
            List<Device> devices = checkNotNull(deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds).get());
            return filterDevicesByReadPermission(devices);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    public List<Device> findByQuery(@RequestBody DeviceSearchQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(getCurrentUser().getTenantId(), query).get());
            return filterDevicesByReadPermission(devices);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<Device> filterDevicesByReadPermission(List<Device> devices) {
        return devices.stream().filter(device -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.DEVICE, Operation.READ, device.getId(), device);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getDeviceTypes() throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId);
            return checkNotNull(deviceTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> claimDevice(@PathVariable(DEVICE_NAME) String deviceName,
                                                      @RequestBody(required = false) ClaimRequest claimRequest) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        try {
            final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();

            Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
            accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                    device.getId(), device);
            String secretKey = getSecretKey(claimRequest);

            ListenableFuture<ClaimResult> future = claimDevicesService.claimDevice(device, customerId, secretKey);
            Futures.addCallback(future, new FutureCallback<ClaimResult>() {
                @Override
                public void onSuccess(@Nullable ClaimResult result) {
                    HttpStatus status;
                    if (result != null) {
                        if (result.getResponse().equals(ClaimResponse.SUCCESS)) {
                            status = HttpStatus.OK;
                            deferredResult.setResult(new ResponseEntity<>(result, status));
                        } else {
                            status = HttpStatus.BAD_REQUEST;
                            if (result.getResponse().equals(ClaimResponse.FAILURE)) {
                                deferredResult.setResult(new ResponseEntity<>(result.getResponse(), status));
                            } else {
                                deferredResult.setResult(new ResponseEntity<>(result, status));
                            }
                        }
                    } else {
                        deferredResult.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
                    }
                }
                @Override
                public void onFailure(Throwable t) {
                    deferredResult.setErrorResult(t);
                }
            });
            return deferredResult;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> reClaimDevice(@PathVariable(DEVICE_NAME) String deviceName) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        try {
            final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();

            Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
            accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                    device.getId(), device);

            ListenableFuture<List<Void>> future = claimDevicesService.reClaimDevice(tenantId, device);
            Futures.addCallback(future, new FutureCallback<List<Void>>() {
                @Override
                public void onSuccess(@Nullable List<Void> result) {
                    if (result != null) {
                        deferredResult.setResult(new ResponseEntity(HttpStatus.OK));
                    } else {
                        deferredResult.setResult(new ResponseEntity(HttpStatus.BAD_REQUEST));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    deferredResult.setErrorResult(t);
                }
            });
            return deferredResult;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String getSecretKey(ClaimRequest claimRequest) throws IOException {
        String secretKey = claimRequest.getSecretKey();
        if (secretKey != null) {
            return secretKey;
        }
        return DataConstants.DEFAULT_SECRET_KEY;
    }
}
