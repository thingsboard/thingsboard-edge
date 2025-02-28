/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.MobileAppId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.dao.mobile.MobileAppDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class MobileAppBundleDataValidator extends DataValidator<MobileAppBundle> {

    @Autowired
    private MobileAppDao mobileAppDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, MobileAppBundle mobileAppBundle) {
        MobileAppId androidAppId = mobileAppBundle.getAndroidAppId();
        if (androidAppId != null) {
            MobileApp androidApp = mobileAppDao.findById(tenantId, androidAppId.getId());
            if (androidApp == null) {
                throw new DataValidationException("Mobile app bundle refers to non-existing android app!");
            }
            if (androidApp.getPlatformType() != PlatformType.ANDROID) {
                throw new DataValidationException("Mobile app bundle refers to wrong android app! Platform type of specified app is " + androidApp.getPlatformType());
            }
        }
        MobileAppId iosAppId = mobileAppBundle.getIosAppId();
        if (iosAppId != null) {
            MobileApp iosApp = mobileAppDao.findById(tenantId, iosAppId.getId());
            if (iosApp == null) {
                throw new DataValidationException("Mobile app bundle refers to non-existing ios app!");
            }
            if (iosApp.getPlatformType() != PlatformType.IOS) {
                throw new DataValidationException("Mobile app bundle refers to wrong ios app! Platform type of specified app is " + iosApp.getPlatformType());
            }
        }
    }
}
