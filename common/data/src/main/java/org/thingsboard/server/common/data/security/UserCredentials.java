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
package org.thingsboard.server.common.data.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Arrays;
import java.util.Objects;

import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo.setJson;

@EqualsAndHashCode(callSuper = true)
public class UserCredentials extends BaseData<UserCredentialsId> {

    private static final long serialVersionUID = -2108436378880529163L;

    private UserId userId;
    private boolean enabled;
    private String password;
    private String activateToken;
    private String resetToken;

    @NoXss
    private transient JsonNode additionalInfo;

    @JsonIgnore
    private byte[] additionalInfoBytes;

    public JsonNode getAdditionalInfo() {
        return getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    public void setAdditionalInfo(JsonNode settings) {
        setJson(settings, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }
    
    public UserCredentials() {
        super();
    }

    public UserCredentials(UserCredentialsId id) {
        super(id);
    }

    public UserCredentials(UserCredentials userCredentials) {
        super(userCredentials);
        this.userId = userCredentials.getUserId();
        this.password = userCredentials.getPassword();
        this.enabled = userCredentials.isEnabled();
        this.activateToken = userCredentials.getActivateToken();
        this.resetToken = userCredentials.getResetToken();
        setAdditionalInfo(userCredentials.getAdditionalInfo());
    }

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getActivateToken() {
        return activateToken;
    }

    public void setActivateToken(String activateToken) {
        this.activateToken = activateToken;
    }
    
    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserCredentials [userId=");
        builder.append(userId);
        builder.append(", enabled=");
        builder.append(enabled);
        builder.append(", password=");
        builder.append(password);
        builder.append(", activateToken=");
        builder.append(activateToken);
        builder.append(", resetToken=");
        builder.append(resetToken);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
