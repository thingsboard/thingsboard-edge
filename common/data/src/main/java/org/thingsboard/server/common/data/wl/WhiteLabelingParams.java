/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    protected String helpLinkBaseUrl;
    protected Boolean enableHelpLinks;
    protected boolean whiteLabelingEnabled = true;
    protected Boolean showNameVersion;
    protected String platformName;
    protected String platformVersion;
    protected String customCss;

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
        if (otherWlParams.helpLinkBaseUrl != null) {
            this.helpLinkBaseUrl = otherWlParams.helpLinkBaseUrl;
        }
        if (otherWlParams.enableHelpLinks != null) {
            this.enableHelpLinks = otherWlParams.enableHelpLinks;
        }
        if (this.showNameVersion == null) {
            this.showNameVersion = otherWlParams.showNameVersion;
            this.platformName = otherWlParams.platformName;
            this.platformVersion = otherWlParams.platformVersion;
        }
        if (!StringUtils.isEmpty(otherWlParams.customCss)) {
            if (StringUtils.isEmpty(this.customCss)) {
                this.customCss = otherWlParams.customCss;
            } else {
                this.customCss = otherWlParams.customCss + "\n" + this.customCss;
            }
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
