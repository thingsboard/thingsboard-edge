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
package org.thingsboard.rule.engine.api.msg;

import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.*;

/**
 * @author Andrew Shvayka
 */
public class DeviceAttributes {

    private final Map<String, AttributeKvEntry> clientSideAttributesMap;
    private final Map<String, AttributeKvEntry> serverPrivateAttributesMap;
    private final Map<String, AttributeKvEntry> serverPublicAttributesMap;

    public DeviceAttributes(List<AttributeKvEntry> clientSideAttributes, List<AttributeKvEntry> serverPrivateAttributes, List<AttributeKvEntry> serverPublicAttributes) {
        this.clientSideAttributesMap = mapAttributes(clientSideAttributes);
        this.serverPrivateAttributesMap = mapAttributes(serverPrivateAttributes);
        this.serverPublicAttributesMap = mapAttributes(serverPublicAttributes);
    }

    private static Map<String, AttributeKvEntry> mapAttributes(List<AttributeKvEntry> attributes) {
        Map<String, AttributeKvEntry> result = new HashMap<>();
        for (AttributeKvEntry attribute : attributes) {
            result.put(attribute.getKey(), attribute);
        }
        return result;
    }

    public Collection<AttributeKvEntry> getClientSideAttributes() {
        return clientSideAttributesMap.values();
    }

    public Collection<AttributeKvEntry> getServerSideAttributes() {
        return serverPrivateAttributesMap.values();
    }

    public Collection<AttributeKvEntry> getServerSidePublicAttributes() {
        return serverPublicAttributesMap.values();
    }

    public Optional<AttributeKvEntry> getClientSideAttribute(String attribute) {
        return Optional.ofNullable(clientSideAttributesMap.get(attribute));
    }

    public Optional<AttributeKvEntry> getServerPrivateAttribute(String attribute) {
        return Optional.ofNullable(serverPrivateAttributesMap.get(attribute));
    }

    public Optional<AttributeKvEntry> getServerPublicAttribute(String attribute) {
        return Optional.ofNullable(serverPublicAttributesMap.get(attribute));
    }

    public void remove(AttributeKey key) {
        Map<String, AttributeKvEntry> map = getMapByScope(key.getScope());
        if (map != null) {
            map.remove(key.getAttributeKey());
        }
    }

    public void update(String scope, List<AttributeKvEntry> values) {
        Map<String, AttributeKvEntry> map = getMapByScope(scope);
        values.forEach(v -> map.put(v.getKey(), v));
    }

    private Map<String, AttributeKvEntry> getMapByScope(String scope) {
        Map<String, AttributeKvEntry> map = null;
        if (scope.equalsIgnoreCase(DataConstants.CLIENT_SCOPE)) {
            map = clientSideAttributesMap;
        } else if (scope.equalsIgnoreCase(DataConstants.SHARED_SCOPE)) {
            map = serverPublicAttributesMap;
        } else if (scope.equalsIgnoreCase(DataConstants.SERVER_SCOPE)) {
            map = serverPrivateAttributesMap;
        }
        return map;
    }

    @Override
    public String toString() {
        return "DeviceAttributes{" +
                "clientSideAttributesMap=" + clientSideAttributesMap +
                ", serverPrivateAttributesMap=" + serverPrivateAttributesMap +
                ", serverPublicAttributesMap=" + serverPublicAttributesMap +
                '}';
    }
}
