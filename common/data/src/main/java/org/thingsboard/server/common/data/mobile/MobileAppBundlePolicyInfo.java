/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.mobile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.MobileAppBundleId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class MobileAppBundlePolicyInfo extends MobileAppBundle {

    @Schema(description = "Terms of use")
    private String termsOfUse;
    @Schema(description = "Privacy policy")
    private String privacyPolicy;

    public MobileAppBundlePolicyInfo() {
        super();
    }

    public MobileAppBundlePolicyInfo(MobileAppBundleId id) {
        super(id);
    }

    public MobileAppBundlePolicyInfo(MobileAppBundle mobileAppBundle) {
        super(mobileAppBundle);
    }

    public MobileAppBundlePolicyInfo(MobileAppBundlePolicyInfo appBundlePolicyInfo) {
        super(appBundlePolicyInfo);
        this.privacyPolicy = appBundlePolicyInfo.getPrivacyPolicy();
        this.termsOfUse = appBundlePolicyInfo.getTermsOfUse();
    }

    public MobileAppBundlePolicyInfo(MobileAppBundle mobileAppBundle, String termsOfUse, String privacyPolicy) {
        super(mobileAppBundle);
        this.termsOfUse = termsOfUse;
        this.privacyPolicy = privacyPolicy;
    }

}
