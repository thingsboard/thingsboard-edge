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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.UUID;

@Data
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Entity
@Table(name = ModelConstants.USER_SETTINGS_COLUMN_FAMILY_NAME)
@IdClass(UserSettingsCompositeKey.class)
public class UserSettingsEntity implements ToData<UserSettings> {

    @Id
    @Column(name = ModelConstants.USER_SETTINGS_USER_ID_PROPERTY)
    private UUID userId;
    @Id
    @Column(name = ModelConstants.USER_SETTINGS_TYPE_PROPERTY)
    private String type;
    @Type(type = "json")
    @Column(name = ModelConstants.USER_SETTINGS_SETTINGS)
    private JsonNode settings;

    public UserSettingsEntity(UserSettings userSettings) {
        this.userId = userSettings.getUserId().getId();
        this.type = userSettings.getType().name();
        if (userSettings.getSettings() != null) {
            this.settings = userSettings.getSettings();
        }
    }

    @Override
    public UserSettings toData() {
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(new UserId(userId));
        userSettings.setType(UserSettingsType.valueOf(type));
        if (settings != null) {
            userSettings.setSettings(settings);
        }
        return userSettings;
    }

}
