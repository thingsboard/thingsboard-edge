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
package org.thingsboard.server.common.data.selfregistration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.permission.GroupPermission;

import java.util.List;

@Data
@EqualsAndHashCode
public class SelfRegistrationParams extends SignUpSelfRegistrationParams {

    private String adminSettingsId;
    private String domainName;
    private String captchaSecretKey;
    private String privacyPolicy;
    private String notificationEmail;
    private String defaultDashboardId;
    private boolean defaultDashboardFullscreen;
    private List<GroupPermission> permissions;

}
