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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.selfregistration.MobileSelfRegistrationParams;
import org.thingsboard.server.dao.model.ModelConstants;

import static org.thingsboard.server.dao.model.ModelConstants.MOBILE_APP_BUNDLE_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = MOBILE_APP_BUNDLE_TABLE_NAME)
public final class MobileAppBundlePolicyInfoEntity extends AbstractMobileAppBundleEntity<MobileAppBundle> {

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_TERMS_OF_USE_PROPERTY)
    private String termsOfUse;

    @Column(name = ModelConstants.MOBILE_APP_BUNDLE_PRIVACY_POLICY_PROPERTY)
    private String privacyPolicy;

    public MobileAppBundlePolicyInfoEntity() {
        super();
    }

    public MobileAppBundlePolicyInfoEntity(MobileAppBundle mobileAppBundle) {
        super(mobileAppBundle);
        MobileSelfRegistrationParams selfRegistrationParams = mobileAppBundle.getSelfRegistrationParams();
        if (selfRegistrationParams != null) {
            this.termsOfUse = selfRegistrationParams.getTermsOfUse();
            this.privacyPolicy = selfRegistrationParams.getPrivacyPolicy();
            selfRegistrationParams.setPrivacyPolicy(null);
            selfRegistrationParams.setTermsOfUse(null);
            this.selfRegistrationConfig = toJson(mobileAppBundle.getSelfRegistrationParams());
        }
    }

    @Override
    public MobileAppBundle toData() {
        MobileAppBundle mobileAppBundle = super.toMobileAppBundle();
        MobileSelfRegistrationParams selfRegistrationParams = mobileAppBundle.getSelfRegistrationParams();
        if (selfRegistrationParams != null) {
            selfRegistrationParams.setPrivacyPolicy(privacyPolicy);
            selfRegistrationParams.setTermsOfUse(termsOfUse);
        }
        return mobileAppBundle;
    }
}
