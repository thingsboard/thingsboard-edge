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
package org.thingsboard.server.common.data.mobile.bundle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.MobileAppBundleId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema
public class MobileAppBundleInfo extends MobileAppBundle {

    @Schema(description = "Android package name")
    private String androidPkgName;
    @Schema(description = "IOS package name")
    private String iosPkgName;
    @Schema(description = "List of available oauth2 clients")
    private List<OAuth2ClientInfo> oauth2ClientInfos;
    @Schema(description = "Indicates if qr code is available for bundle")
    private boolean qrCodeEnabled;

    public MobileAppBundleInfo(MobileAppBundle mobileApp, String androidPkgName, String iosPkgName, boolean qrCodeEnabled) {
        super(mobileApp);
        this.androidPkgName = androidPkgName;
        this.iosPkgName = iosPkgName;
        this.qrCodeEnabled = qrCodeEnabled;
    }

    public MobileAppBundleInfo(MobileAppBundle mobileApp, String androidPkgName, String iosPkgName, boolean qrCodeEnabled, List<OAuth2ClientInfo> oauth2ClientInfos) {
        super(mobileApp);
        this.androidPkgName = androidPkgName;
        this.iosPkgName = iosPkgName;
        this.qrCodeEnabled = qrCodeEnabled;
        this.oauth2ClientInfos = oauth2ClientInfos;
    }

    public MobileAppBundleInfo(MobileAppBundle mobileApp, List<OAuth2ClientInfo> oauth2ClientInfos) {
        super(mobileApp);
        this.oauth2ClientInfos = oauth2ClientInfos;
    }

    public MobileAppBundleInfo() {
        super();
    }

    public MobileAppBundleInfo(MobileAppBundleId mobileAppBundleId) {
        super(mobileAppBundleId);
    }

}
