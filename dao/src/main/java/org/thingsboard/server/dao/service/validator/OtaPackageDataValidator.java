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
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.ota.OtaPackageDao;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import static org.thingsboard.server.common.data.EntityType.OTA_PACKAGE;

@Component
public class OtaPackageDataValidator extends BaseOtaPackageDataValidator<OtaPackage> {

    @Autowired
    private OtaPackageDao otaPackageDao;

    @Autowired
    @Lazy
    private OtaPackageService otaPackageService;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, OtaPackage otaPackage) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxOtaPackagesInBytes = profileConfiguration.getMaxOtaPackagesInBytes();
        validateMaxSumDataSizePerTenant(tenantId, otaPackageDao, maxOtaPackagesInBytes, otaPackage.getDataSize(), OTA_PACKAGE);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, OtaPackage otaPackage) {
        validateImpl(otaPackage);

        if (!otaPackage.hasUrl()) {
            if (StringUtils.isEmpty(otaPackage.getFileName())) {
                throw new DataValidationException("OtaPackage file name should be specified!");
            }

            if (StringUtils.isEmpty(otaPackage.getContentType())) {
                throw new DataValidationException("OtaPackage content type should be specified!");
            }

            if (otaPackage.getChecksumAlgorithm() == null) {
                throw new DataValidationException("OtaPackage checksum algorithm should be specified!");
            }
            if (StringUtils.isEmpty(otaPackage.getChecksum())) {
                throw new DataValidationException("OtaPackage checksum should be specified!");
            }

            String currentChecksum;

            currentChecksum = otaPackageService.generateChecksum(otaPackage.getChecksumAlgorithm(), otaPackage.getData());

            if (!currentChecksum.equals(otaPackage.getChecksum())) {
                throw new DataValidationException("Wrong otaPackage file!");
            }
        } else {
            if (otaPackage.getData() != null) {
                throw new DataValidationException("File can't be saved if URL present!");
            }
        }
    }

    @Override
    protected OtaPackage validateUpdate(TenantId tenantId, OtaPackage otaPackage) {
        OtaPackage otaPackageOld = otaPackageDao.findById(tenantId, otaPackage.getUuidId());

        validateUpdate(otaPackage, otaPackageOld);

        if (otaPackageOld.getData() != null && !otaPackageOld.getData().equals(otaPackage.getData())) {
            throw new DataValidationException("Updating otaPackage data is prohibited!");
        }

        if (otaPackageOld.getData() == null && otaPackage.getData() != null) {
            DefaultTenantProfileConfiguration profileConfiguration =
                    (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
            long maxOtaPackagesInBytes = profileConfiguration.getMaxOtaPackagesInBytes();
            validateMaxSumDataSizePerTenant(tenantId, otaPackageDao, maxOtaPackagesInBytes, otaPackage.getDataSize(), OTA_PACKAGE);
        }
        return otaPackageOld;
    }
}
