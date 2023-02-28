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
package org.thingsboard.server.service.solutions.data.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.service.solutions.data.names.RandomNameData;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class CustomerDefinition extends BaseEntityDefinition {

    private String group;
    private String email;
    private String country;
    private String city;
    private String state;
    private String zip;
    private String address;

    private List<String> assetGroups = Collections.emptyList();
    private List<String> deviceGroups = Collections.emptyList();
    private List<UserGroupDefinition> userGroups = Collections.emptyList();
    private List<UserDefinition> users = Collections.emptyList();

    @JsonIgnore
    private RandomNameData randomNameData;

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

    public void setAssetGroups(List<String> assetGroups) {
        if (assetGroups != null) {
            this.assetGroups = assetGroups;
        }
    }

    public void setDeviceGroups(List<String> deviceGroups) {
        if (deviceGroups != null) {
            this.deviceGroups = deviceGroups;
        }
    }

    public void setUserGroups(List<UserGroupDefinition> userGroups) {
        if (userGroups != null) {
            this.userGroups = userGroups;
        }
    }

    public void setUsers(List<UserDefinition> users) {
        if (users != null) {
            this.users = users;
        }
    }
}
