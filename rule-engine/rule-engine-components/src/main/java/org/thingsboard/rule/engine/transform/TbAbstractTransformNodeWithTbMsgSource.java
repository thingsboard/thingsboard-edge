/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.util.TbPair;

public abstract class TbAbstractTransformNodeWithTbMsgSource implements TbNode {

    protected static final String FROM_METADATA_PROPERTY = "fromMetadata";

    protected abstract String getNewKeyForUpgradeFromVersionZero();

    protected abstract String getKeyToUpgradeFromVersionOne();

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        ObjectNode configToUpdate = (ObjectNode) oldConfiguration;
        switch (fromVersion) {
            case 0:
                return upgradeToUseTbMsgSource(configToUpdate);
            case 1:
                return upgradeNodesWithVersionOneToUseTbMsgSource(configToUpdate);
            default:
                return new TbPair<>(false, oldConfiguration);
        }
    }

    private TbPair<Boolean, JsonNode> upgradeToUseTbMsgSource(ObjectNode configToUpdate) throws TbNodeException {
        if (!configToUpdate.has(FROM_METADATA_PROPERTY)) {
            throw new TbNodeException("property to update: '" + FROM_METADATA_PROPERTY + "' doesn't exists in configuration!");
        }
        var value = configToUpdate.get(FROM_METADATA_PROPERTY).asText();
        if ("true".equals(value)) {
            configToUpdate.remove(FROM_METADATA_PROPERTY);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.METADATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        if ("false".equals(value)) {
            configToUpdate.remove(FROM_METADATA_PROPERTY);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.DATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        throw new TbNodeException("property to update: '" + FROM_METADATA_PROPERTY + "' has unexpected value: "
                + value + ". Allowed values: true or false!");
    }

    private TbPair<Boolean, JsonNode> upgradeNodesWithVersionOneToUseTbMsgSource(ObjectNode configToUpdate) throws TbNodeException {
        if (configToUpdate.has(getNewKeyForUpgradeFromVersionZero())) {
            return new TbPair<>(false, configToUpdate);
        }
        return upgradeTbMsgSourceKey(configToUpdate, getKeyToUpgradeFromVersionOne());
    }

    private TbPair<Boolean, JsonNode> upgradeTbMsgSourceKey(ObjectNode configToUpdate, String oldPropertyKey) throws TbNodeException {
        if (!configToUpdate.has(oldPropertyKey)) {
            throw new TbNodeException("property to update: '" + oldPropertyKey + "' doesn't exists in configuration!");
        }
        var value = configToUpdate.get(oldPropertyKey).asText();
        if (TbMsgSource.METADATA.name().equals(value)) {
            configToUpdate.remove(oldPropertyKey);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.METADATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        if (TbMsgSource.DATA.name().equals(value)) {
            configToUpdate.remove(oldPropertyKey);
            configToUpdate.put(getNewKeyForUpgradeFromVersionZero(), TbMsgSource.DATA.name());
            return new TbPair<>(true, configToUpdate);
        }
        throw new TbNodeException("property to update: '" + oldPropertyKey + "' has unexpected value: "
                + value + ". Allowed values: true or false!");
    }

}
