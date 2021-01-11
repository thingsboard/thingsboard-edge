/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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

@Data
@EqualsAndHashCode
public class LoginWhiteLabelingParams extends WhiteLabelingParams {

    private String pageBackgroundColor;
    private boolean darkForeground;
    private String domainName;
    private String baseUrl;
    private boolean prohibitDifferentUrl;
    private String adminSettingsId;
    private Boolean showNameBottom;

    public LoginWhiteLabelingParams merge(LoginWhiteLabelingParams otherWlParams) {
        Integer prevLogoImageHeight = this.logoImageHeight;
        super.merge(otherWlParams);
        if (prevLogoImageHeight == null) {
            this.logoImageHeight = null;
        }
        if (this.showNameBottom == null) {
            this.showNameBottom = otherWlParams.showNameBottom;
        }
        return this;
    }
}
