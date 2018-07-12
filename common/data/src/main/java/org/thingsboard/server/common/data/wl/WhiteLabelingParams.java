/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.wl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

@Data
@EqualsAndHashCode
public class WhiteLabelingParams {

    protected String logoImageUrl;
    protected String logoImageChecksum;
    protected Integer logoImageHeight;
    protected String appTitle;
    protected Favicon favicon;
    protected String faviconChecksum;
    protected PaletteSettings paletteSettings;

    public WhiteLabelingParams merge(WhiteLabelingParams otherWlParams) {
        if (StringUtils.isEmpty(this.logoImageUrl)) {
            this.logoImageUrl = otherWlParams.logoImageUrl;
            this.logoImageChecksum = otherWlParams.logoImageChecksum;
        }
        if (this.logoImageHeight == null) {
            this.logoImageHeight = otherWlParams.logoImageHeight;
        }
        if (StringUtils.isEmpty(appTitle)) {
            this.appTitle = otherWlParams.appTitle;
        }
        if (favicon == null || StringUtils.isEmpty(favicon.getUrl())) {
            this.favicon = otherWlParams.favicon;
            this.faviconChecksum = otherWlParams.faviconChecksum;
        }
        if (this.paletteSettings == null) {
            this.paletteSettings = otherWlParams.paletteSettings;
        } else if (otherWlParams.paletteSettings != null) {
            this.paletteSettings.merge(otherWlParams.paletteSettings);
        }
        return this;
    }

    public void prepareImages(String logoImageChecksum, String faviconChecksum) {
        if (!StringUtils.isEmpty(logoImageChecksum) && logoImageChecksum.equals(this.logoImageChecksum)) {
            this.logoImageUrl = null;
        }
        if (!StringUtils.isEmpty(faviconChecksum) && faviconChecksum.equals(this.faviconChecksum)) {
            this.favicon = null;
        }
    }

}
