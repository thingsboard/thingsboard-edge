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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasOtaPackage;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

public abstract class AbstractHasOtaPackageValidator<D extends BaseData<?>> extends DataValidator<D> {

    @Autowired
    @Lazy
    private OtaPackageService otaPackageService;

    protected <T extends HasOtaPackage> void validateOtaPackage(TenantId tenantId, T entity, DeviceProfileId deviceProfileId) {
        if (entity.getFirmwareId() != null) {
            OtaPackage firmware = otaPackageService.findOtaPackageById(tenantId, entity.getFirmwareId());
            validateOtaPackage(tenantId, OtaPackageType.FIRMWARE, deviceProfileId, firmware);
        }
        if (entity.getSoftwareId() != null) {
            OtaPackage software = otaPackageService.findOtaPackageById(tenantId, entity.getSoftwareId());
            validateOtaPackage(tenantId, OtaPackageType.SOFTWARE, deviceProfileId, software);
        }
    }

    private void validateOtaPackage(TenantId tenantId, OtaPackageType type, DeviceProfileId deviceProfileId, OtaPackage otaPackage) {
        if (otaPackage == null) {
            throw new DataValidationException(prepareMsg("Can't assign non-existent %s!", type));
        }
        if (!otaPackage.getTenantId().equals(tenantId)) {
            throw new DataValidationException(prepareMsg("Can't assign %s from different tenant!", type));
        }
        if (!otaPackage.getType().equals(type)) {
            throw new DataValidationException(prepareMsg("Can't assign %s with type: " + otaPackage.getType(), type));
        }
        if (otaPackage.getData() == null && !otaPackage.hasUrl()) {
            throw new DataValidationException(prepareMsg("Can't assign %s with empty data!", type));
        }
        if (!otaPackage.getDeviceProfileId().equals(deviceProfileId)) {
            throw new DataValidationException(prepareMsg("Can't assign %s with different deviceProfile!", type));
        }
    }

    private String prepareMsg(String msg, OtaPackageType type) {
        return String.format(msg, type.name().toLowerCase());
    }
}
