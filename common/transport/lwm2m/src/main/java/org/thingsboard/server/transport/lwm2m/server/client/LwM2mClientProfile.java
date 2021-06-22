/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.server.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2MClientStrategy;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2MFirmwareUpdateStrategy;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LwM2MSoftwareUpdateStrategy;

@Data
public class LwM2mClientProfile {
    private final String clientStrategyStr = "clientStrategy";
    private final String fwUpdateStrategyStr = "fwUpdateStrategy";
    private final String swUpdateStrategyStr = "swUpdateStrategy";

    private TenantId tenantId;
    /**
     *   "clientLwM2mSettings": {
     *     "fwUpdateStrategy": "1",
     *     "swUpdateStrategy": "1",
     *     "clientStrategy": "1"
     *   }
     **/
    private JsonObject postClientLwM2mSettings;

    /**
     * {"keyName": {
     *       "/3_1.0/0/1": "modelNumber",
     *       "/3_1.0/0/0": "manufacturer",
     *       "/3_1.0/0/2": "serialNumber"
     *       }
     **/
    private JsonObject postKeyNameProfile;

    /**
     * [ "/3_1.0/0/0", "/3_1.0/0/1"]
     */
    private JsonArray postAttributeProfile;

    /**
     * [ "/3_1.0/0/0", "/3_1.0/0/2"]
     */
    private JsonArray postTelemetryProfile;

    /**
     * [ "/3_1.0/0", "/3_1.0/0/1, "/3_1.0/0/2"]
     */
    private JsonArray postObserveProfile;

    /**
     * "attributeLwm2m": {"/3_1.0": {"ver": "currentTimeTest11"},
     *                    "/3_1.0/0": {"gt": 17},
     *                    "/3_1.0/0/9": {"pmax": 45}, "/3_1.2": {ver": "3_1.2"}}
     */
    private JsonObject postAttributeLwm2mProfile;

    public LwM2mClientProfile clone() {
        LwM2mClientProfile lwM2mClientProfile = new LwM2mClientProfile();
        lwM2mClientProfile.postClientLwM2mSettings = this.deepCopy(this.postClientLwM2mSettings, JsonObject.class);
        lwM2mClientProfile.postKeyNameProfile = this.deepCopy(this.postKeyNameProfile, JsonObject.class);
        lwM2mClientProfile.postAttributeProfile = this.deepCopy(this.postAttributeProfile, JsonArray.class);
        lwM2mClientProfile.postTelemetryProfile = this.deepCopy(this.postTelemetryProfile, JsonArray.class);
        lwM2mClientProfile.postObserveProfile = this.deepCopy(this.postObserveProfile, JsonArray.class);
        lwM2mClientProfile.postAttributeLwm2mProfile = this.deepCopy(this.postAttributeLwm2mProfile, JsonObject.class);
        return lwM2mClientProfile;
    }


    private <T> T deepCopy(T elements, Class<T> type) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(gson.toJson(elements), type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getClientStrategy() {
        return this.postClientLwM2mSettings.getAsJsonObject().has(this.clientStrategyStr) ?
                Integer.parseInt(this.postClientLwM2mSettings.getAsJsonObject().get(this.clientStrategyStr).getAsString()) :
                LwM2MClientStrategy.CLIENT_STRATEGY_1.code;
    }

    public int getFwUpdateStrategy() {
        return this.postClientLwM2mSettings.getAsJsonObject().has(this.fwUpdateStrategyStr) ?
                Integer.parseInt(this.postClientLwM2mSettings.getAsJsonObject().get(this.fwUpdateStrategyStr).getAsString()) :
                LwM2MFirmwareUpdateStrategy.OBJ_5_BINARY.code;
    }

    public int getSwUpdateStrategy() {
        return this.postClientLwM2mSettings.getAsJsonObject().has(this.swUpdateStrategyStr) ?
                Integer.parseInt(this.postClientLwM2mSettings.getAsJsonObject().get(this.swUpdateStrategyStr).getAsString()) :
                LwM2MSoftwareUpdateStrategy.BINARY.code;
    }
}
