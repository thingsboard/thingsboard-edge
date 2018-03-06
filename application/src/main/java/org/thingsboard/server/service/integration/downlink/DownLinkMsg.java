/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.service.integration.downlink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.service.integration.msg.RPCCallIntegrationMsg;
import org.thingsboard.server.service.integration.msg.SharedAttributesUpdateIntegrationMsg;

import java.io.Serializable;
import java.util.*;

/**
 * Created by ashvayka on 22.02.18.
 */
@Data
public class DownLinkMsg implements Serializable {

    private final DeviceId deviceId;
    private final String deviceName;
    private final String deviceType;

    private Map<String, Map<String,String>> currentAttributes = new HashMap<>();
    {
        currentAttributes.put("server", new HashMap<>());
        currentAttributes.put("shared", new HashMap<>());
        currentAttributes.put("client", new HashMap<>());
    }
    private Set<String> deletedAttributes = new HashSet<>();
    private Map<String, AttributeUpdate> updatedAttributes = new HashMap<>();
    private List<RPCCall> rpcCalls = new LinkedList<>();

    public static DownLinkMsg from(RPCCallIntegrationMsg msg) {
        return merge(new DownLinkMsg(msg.getDeviceId(), msg.getDeviceName(), msg.getDeviceType()), msg);
    }

    public static DownLinkMsg merge(DownLinkMsg result, RPCCallIntegrationMsg msg) {
        RPCCall call = new RPCCall();
        call.setId(msg.getId());
        call.setExpirationTime(msg.getExpirationTime());
        call.setMethod(msg.getBody().getMethod());
        call.setParams(msg.getBody().getParams());
        result.rpcCalls.add(call);
        return result;
    }

    public static DownLinkMsg from(SharedAttributesUpdateIntegrationMsg msg) {
        return merge(new DownLinkMsg(msg.getDeviceId(), msg.getDeviceName(), msg.getDeviceType()), msg);
    }

    public static DownLinkMsg merge(DownLinkMsg result, SharedAttributesUpdateIntegrationMsg msg) {
        if (msg.getDeletedKeys() != null) {
            msg.getDeletedKeys().forEach(key -> result.deletedAttributes.add(key.getAttributeKey()));
        }

        if (msg.getUpdatedValues() != null) {
            msg.getUpdatedValues().forEach(value -> result.getUpdatedAttributes().put(value.getKey(), new AttributeUpdate(value.getLastUpdateTs(), value.getValueAsString())));
        }

        return result;
    }

    public void addCurrentAttribute(String scope, String key, String value) {
        String scopeKey = "";
        if (DataConstants.SERVER_SCOPE.equals(scope)) {
            scopeKey = "server";
        } else if (DataConstants.SHARED_SCOPE.equals(scope)) {
            scopeKey = "shared";
        } else if (DataConstants.CLIENT_SCOPE.equals(scope)) {
            scopeKey = "client";
        }
        if (!StringUtils.isEmpty(scopeKey)) {
            currentAttributes.get(scopeKey).put(key, value);
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return deletedAttributes.isEmpty() && updatedAttributes.isEmpty() && rpcCalls.isEmpty();
    }
}
