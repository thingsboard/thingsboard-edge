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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Arrays;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "related entity data",
        configClazz = TbGetRelatedDataNodeConfiguration.class,
        nodeDescription = "Adds originators related entity attributes or latest telemetry or fields into message or message metadata",
        nodeDetails = "Related entity lookup based on the configured relation query. " +
                "If multiple related entities are found, only first entity is used for message enrichment, other entities are discarded. " +
                "Useful when you need to retrieve data from an entity that has a relation to the message originator and use them for further message processing.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeRelatedAttributesConfig")
public class TbGetRelatedAttributeNode extends TbAbstractGetEntityDataNode<EntityId> {

    private static final String RELATED_ENTITY_NOT_FOUND_MESSAGE = "Failed to find related entity to message originator using relation query specified in the configuration!";

    @Override
    public TbGetRelatedDataNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetRelatedDataNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getDataMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        return config;
    }

    @Override
    public ListenableFuture<EntityId> findEntityAsync(TbContext ctx, EntityId originator) {
        var relatedAttrConfig = (TbGetRelatedDataNodeConfiguration) config;
        return Futures.transformAsync(
                EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, originator, relatedAttrConfig.getRelationsQuery()),
                checkIfEntityIsPresentOrThrow(RELATED_ENTITY_NOT_FOUND_MESSAGE),
                ctx.getDbCallbackExecutor());
    }

    @Override
    protected void checkDataToFetchSupportedOrElseThrow(DataToFetch dataToFetch) throws TbNodeException {
        if (dataToFetch == null) {
            throw new TbNodeException("DataToFetch property cannot be null! Supported values are: " + Arrays.toString(DataToFetch.values()));
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ? upgradeToUseFetchToAndDataToFetch(oldConfiguration) : new TbPair<>(false, oldConfiguration);
    }

}
