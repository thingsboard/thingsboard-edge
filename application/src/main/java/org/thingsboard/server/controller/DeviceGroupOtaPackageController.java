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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class DeviceGroupOtaPackageController extends BaseController {

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceGroupOtaPackage/{groupId}/{firmwareType}", method = RequestMethod.GET)
    public DeviceGroupOtaPackage getFirmwareById(@PathVariable("groupId") String strGroupId,
                                                 @PathVariable("firmwareType") String strFirmwareType) throws ThingsboardException {
        checkParameter("groupId", strGroupId);
        checkParameter("firmwareType", strFirmwareType);
        EntityGroupId groupId = new EntityGroupId(toUUID(strGroupId));
        checkEntityGroupId(groupId, Operation.READ);
        return deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(groupId, OtaPackageType.valueOf(strFirmwareType));
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceGroupOtaPackage", method = RequestMethod.POST)
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(@RequestBody DeviceGroupOtaPackage deviceGroupOtaPackage) throws ThingsboardException {
        checkEntityGroupId(deviceGroupOtaPackage.getGroupId(), Operation.WRITE);
        DeviceGroupOtaPackage oldDeviceGroupOtaPackage = null;
        if (deviceGroupOtaPackage.getId() != null) {
            oldDeviceGroupOtaPackage = deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(deviceGroupOtaPackage.getId());
        }
        DeviceGroupOtaPackage savedDeviceGroupOtaPackage = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(getTenantId(), deviceGroupOtaPackage);
        otaPackageStateService.update(getTenantId(), savedDeviceGroupOtaPackage, oldDeviceGroupOtaPackage);
        return savedDeviceGroupOtaPackage;
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceGroupOtaPackage/{id}", method = RequestMethod.DELETE)
    public void deleteDeviceGroupOtaPackage(@PathVariable("id") String strId) throws ThingsboardException {
        checkParameter("deviceGroupOtaPackageId", strId);
        UUID id = toUUID(strId);
        DeviceGroupOtaPackage deviceGroupOtaPackage = deviceGroupOtaPackageService.findDeviceGroupOtaPackageById(id);
        checkEntityGroupId(deviceGroupOtaPackage.getGroupId(), Operation.WRITE);
        deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(id);
        otaPackageStateService.update(getTenantId(), null, deviceGroupOtaPackage);
    }

}
