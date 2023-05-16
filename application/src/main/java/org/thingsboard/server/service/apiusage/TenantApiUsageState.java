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
package org.thingsboard.server.service.apiusage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TenantApiUsageState extends BaseApiUsageState {
    @Getter
    @Setter
    private TenantProfileId tenantProfileId;
    @Getter
    @Setter
    private TenantProfileData tenantProfileData;

    public TenantApiUsageState(TenantProfile tenantProfile, ApiUsageState apiUsageState) {
        super(apiUsageState);
        this.tenantProfileId = tenantProfile.getId();
        this.tenantProfileData = tenantProfile.getProfileData();
    }

    public TenantApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    public long getProfileThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getProfileThreshold(key);
    }

    public boolean getProfileFeatureEnabled(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getProfileFeatureEnabled(key);
    }

    public long getProfileWarnThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getWarnThreshold(key);
    }

    private Pair<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(ApiFeature feature) {
        ApiUsageStateValue featureValue = ApiUsageStateValue.ENABLED;
        for (ApiUsageRecordKey recordKey : ApiUsageRecordKey.getKeys(feature)) {
            long value = get(recordKey);
            boolean featureEnabled = getProfileFeatureEnabled(recordKey);
            long threshold = getProfileThreshold(recordKey);
            long warnThreshold = getProfileWarnThreshold(recordKey);
            ApiUsageStateValue tmpValue;
            if (featureEnabled) {
                if (threshold == 0 || value == 0 || value < warnThreshold) {
                    tmpValue = ApiUsageStateValue.ENABLED;
                } else if (value < threshold) {
                    tmpValue = ApiUsageStateValue.WARNING;
                } else {
                    tmpValue = ApiUsageStateValue.DISABLED;
                }
            } else {
                tmpValue = ApiUsageStateValue.DISABLED;
            }
            featureValue = ApiUsageStateValue.toMoreRestricted(featureValue, tmpValue);
        }
        return setFeatureValue(feature, featureValue) ? Pair.of(feature, featureValue) : null;
    }


    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThresholds() {
        return checkStateUpdatedDueToThreshold(new HashSet<>(Arrays.asList(ApiFeature.values())));
    }

    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(Set<ApiFeature> features) {
        Map<ApiFeature, ApiUsageStateValue> result = new HashMap<>();
        for (ApiFeature feature : features) {
            Pair<ApiFeature, ApiUsageStateValue> tmp = checkStateUpdatedDueToThreshold(feature);
            if (tmp != null) {
                result.put(tmp.getFirst(), tmp.getSecond());
            }
        }
        return result;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
