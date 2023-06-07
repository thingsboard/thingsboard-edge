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
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.thingsboard.common.util.JacksonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class OpcUaDevice {

    private final NodeId nodeId;
    private final DeviceMapping mapping;
    private final Map<String,String> deviceMetadata;
    private final Map<String, NodeId> tagKeysMap = new HashMap<>();
    private final Map<NodeId, String> tagIdsMap = new HashMap<>();
    private final Map<String, String> tagValues = new HashMap<>();
    private final Map<NodeId, List<SubscriptionTag>> subscriptionTagsMap = new HashMap<>();

    private long scanTs;

    public OpcUaDevice(OpcUaNode node, DeviceMapping mapping) {
        this.nodeId = node.getNodeId();
        this.mapping = mapping;
        this.deviceMetadata = new HashMap<>();
        this.deviceMetadata.put("opcUaNode_identifier", node.getNodeId().getIdentifier().toString());
        this.deviceMetadata.put("opcUaNode_namespaceIndex", node.getNodeId().getNamespaceIndex().toString());
        this.deviceMetadata.put("opcUaNode_name", node.getName());
        this.deviceMetadata.put("opcUaNode_fqn", node.getFqn());
    }

    public Map<String, NodeId> registerTags(Map<String, NodeId> newTagMap) {
        Map<String, NodeId> newTags = new HashMap<>();
        for (Map.Entry<String, NodeId> kv : newTagMap.entrySet()) {
            NodeId old = registerTag(kv);
            if (old == null) {
                newTags.put(kv.getKey(), kv.getValue());
            }
        }
        return newTags;
    }

    private NodeId registerTag(Map.Entry<String, NodeId> kv) {
        String tag = kv.getKey();
        NodeId tagId = kv.getValue();
        mapping.getSubscriptionTags().stream().filter(subscriptionTag -> subscriptionTag.getPath().equals(tag))
                .forEach(subscriptionTag -> subscriptionTagsMap.computeIfAbsent(tagId, key -> new ArrayList<>()).add(subscriptionTag));
        tagIdsMap.putIfAbsent(kv.getValue(), kv.getKey());
        return tagKeysMap.put(kv.getKey(), kv.getValue());
    }

    public void updateTag(NodeId tagId, DataValue dataValue) {
        String tag = tagIdsMap.get(tagId);
        if (dataValue != null && dataValue.getValue() != null && dataValue.getValue().getValue() != null) {
            tagValues.put(tag, dataValue.getValue().getValue().toString());
        }
    }

    public void updateScanTs() {
        scanTs = System.currentTimeMillis();
    }

    public OpcUaIntegrationMsg prepareMsg(NodeId affectedTagId) {
        JsonNode payload = preparePayload(affectedTagId);
        return new OpcUaIntegrationMsg(payload, this.deviceMetadata);
    }

    private JsonNode preparePayload(NodeId affectedTagId) {
        String affectedTagName = null;
        if (affectedTagId != null) {
            affectedTagName = tagIdsMap.get(affectedTagId);
        }
        return preparePayload(affectedTagName);
    }

    private JsonNode preparePayload(String affectedTagName) {
        ObjectNode payload = JacksonUtil.newObjectNode();
        mapping.getSubscriptionTags().forEach(subscriptionTag -> {
            if (affectedTagName == null ||
                    subscriptionTag.isRequired() ||
                    (affectedTagName != null && subscriptionTag.getPath().equals(affectedTagName))) {
                String key = subscriptionTag.getKey();
                String value = tagValues.get(subscriptionTag.getPath());
                payload.put(key, value);
            }
        });
        return payload;
    }
}
