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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Objects;

public abstract class BaseOtaPackageDataValidator<D extends BaseData<?>> extends DataValidator<D> {

    @Autowired
    @Lazy
    private TenantService tenantService;

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    protected void validateImpl(OtaPackageInfo otaPackageInfo) {
        if (otaPackageInfo.getTenantId() == null) {
            throw new DataValidationException("OtaPackage should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(otaPackageInfo.getTenantId())) {
                throw new DataValidationException("OtaPackage is referencing to non-existent tenant!");
            }
        }

        if (otaPackageInfo.getDeviceProfileId() != null) {
            DeviceProfile deviceProfile = deviceProfileDao.findById(otaPackageInfo.getTenantId(), otaPackageInfo.getDeviceProfileId().getId());
            if (deviceProfile == null) {
                throw new DataValidationException("OtaPackage is referencing to non-existent device profile!");
            }
        }

        if (otaPackageInfo.getType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (StringUtils.isEmpty(otaPackageInfo.getTitle())) {
            throw new DataValidationException("OtaPackage title should be specified!");
        }

        if (StringUtils.isEmpty(otaPackageInfo.getVersion())) {
            throw new DataValidationException("OtaPackage version should be specified!");
        }

        if (otaPackageInfo.getTitle().length() > 255) {
            throw new DataValidationException("The length of title should be equal or shorter than 255");
        }

        if (otaPackageInfo.getVersion().length() > 255) {
            throw new DataValidationException("The length of version should be equal or shorter than 255");
        }
    }

    protected void validateUpdate(OtaPackageInfo otaPackage, OtaPackageInfo otaPackageOld) {
        if (!otaPackageOld.getType().equals(otaPackage.getType())) {
            throw new DataValidationException("Updating type is prohibited!");
        }

        if (!otaPackageOld.getTitle().equals(otaPackage.getTitle())) {
            throw new DataValidationException("Updating otaPackage title is prohibited!");
        }

        if (!otaPackageOld.getVersion().equals(otaPackage.getVersion())) {
            throw new DataValidationException("Updating otaPackage version is prohibited!");
        }

        if (!Objects.equals(otaPackage.getTag(), otaPackageOld.getTag())) {
            throw new DataValidationException("Updating otaPackage tag is prohibited!");
        }

        if (!otaPackageOld.getDeviceProfileId().equals(otaPackage.getDeviceProfileId())) {
            throw new DataValidationException("Updating otaPackage deviceProfile is prohibited!");
        }

        if (otaPackageOld.getFileName() != null && !otaPackageOld.getFileName().equals(otaPackage.getFileName())) {
            throw new DataValidationException("Updating otaPackage file name is prohibited!");
        }

        if (otaPackageOld.getContentType() != null && !otaPackageOld.getContentType().equals(otaPackage.getContentType())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksumAlgorithm() != null && !otaPackageOld.getChecksumAlgorithm().equals(otaPackage.getChecksumAlgorithm())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksum() != null && !otaPackageOld.getChecksum().equals(otaPackage.getChecksum())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getDataSize() != null && !otaPackageOld.getDataSize().equals(otaPackage.getDataSize())) {
            throw new DataValidationException("Updating otaPackage data size is prohibited!");
        }

        if (otaPackageOld.getUrl() != null && !otaPackageOld.getUrl().equals(otaPackage.getUrl())) {
            throw new DataValidationException("Updating otaPackage URL is prohibited!");
        }
    }

}
