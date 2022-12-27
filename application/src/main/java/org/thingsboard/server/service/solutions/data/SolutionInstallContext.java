/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.solutions.data;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.service.solutions.data.definition.AssetDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceDefinition;
import org.thingsboard.server.service.solutions.data.definition.EdgeDefinition;
import org.thingsboard.server.service.solutions.data.definition.EntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.EntitySearchKey;
import org.thingsboard.server.service.solutions.data.definition.RelationDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserDefinition;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SolutionInstallContext {

    private final TenantId tenantId;
    private final String solutionId;
    private final TenantSolutionTemplateInstructions solutionInstructions;
    private final List<EntityId> createdEntitiesList = new ArrayList<>();
    private final Map<String, String> realIds = new HashMap<>();
    private final Map<EntitySearchKey, EntityId> entityIdMap = new HashMap<>();
    private final Map<EntityId, List<RelationDefinition>> relationDefinitions = new LinkedHashMap<>();

    // For instructions
    private final Map<String, DeviceCredentialsInfo> createdDevices = new LinkedHashMap<>();
    private final Map<String, UserCredentialsInfo> createdUsers = new LinkedHashMap<>();
    private final Map<String, CreatedEntityInfo> createdEntities = new LinkedHashMap<>();
    private final List<DashboardLinkInfo> dashboardLinks = new ArrayList<>();

    public SolutionInstallContext(TenantId tenantId, String solutionId, TenantSolutionTemplateInstructions solutionInstructions) {
        this.tenantId = tenantId;
        this.solutionId = solutionId;
        this.solutionInstructions = solutionInstructions;
        put(new EntitySearchKey(tenantId, EntityType.TENANT, null, false), tenantId);
    }

    public void registerReferenceOnly(String referenceId, EntityId entityId) {
        if (!StringUtils.isEmpty(referenceId)) {
            realIds.put(referenceId, entityId.getId().toString());
        }
    }

    public void register(String referenceId, EntityId entityId) {
        registerReferenceOnly(referenceId, entityId);
        register(entityId);
    }

    public void register(EntityId entityId) {
        createdEntitiesList.add(entityId);
    }

    public void register(CustomerDefinition definition, Customer customer) {
        register(definition.getJsonId(), customer.getId());
        createdEntities.put(customer.getName(), new CreatedEntityInfo(customer.getName(), "Customer", "Tenant"));
    }

    public void register(CustomerDefinition cDef, UserDefinition definition, User user) {
        register(definition.getJsonId(), user.getId());
        createdEntities.put(user.getName(), new CreatedEntityInfo(user.getName(), "User", StringUtils.isEmpty(cDef.getName()) ? "Tenant" : cDef.getName()));
    }

    public void register(AssetDefinition definition, Asset asset) {
        register(definition.getJsonId(), asset.getId());
        createdEntities.put(asset.getName(), new CreatedEntityInfo(asset.getName(), "Asset", StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void register(DeviceDefinition definition, Device device) {
        register(definition.getJsonId(), device.getId());
        createdEntities.put(device.getName(), new CreatedEntityInfo(device.getName(), "Device", StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void register(DashboardDefinition definition, Dashboard dashboard) {
        register(definition.getJsonId(), dashboard.getId());
        createdEntities.put(dashboard.getName(), new CreatedEntityInfo(dashboard.getName(), "Dashboard", StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void register(String referenceId, RuleChain ruleChain) {
        register(referenceId, ruleChain.getId());
        createdEntities.put(ruleChain.getName(), new CreatedEntityInfo(ruleChain.getName(), "Rule chain", "Tenant"));
    }


    public void register(Role role) {
        register(role.getId());
        createdEntities.put(role.getName(), new CreatedEntityInfo(role.getName(), "Role", "Tenant"));
    }

    public void register(DeviceProfile deviceProfile) {
        register(deviceProfile.getId());
        createdEntities.put(deviceProfile.getName(), new CreatedEntityInfo(deviceProfile.getName(), "Device profile", "Tenant"));
    }

    public void register(AssetProfile assetProfile) {
        register(assetProfile.getId());
        createdEntities.put(assetProfile.getName(), new CreatedEntityInfo(assetProfile.getName(), "Asset profile", "Tenant"));
    }

    public void register(EdgeDefinition definition, Edge edge) {
        register(definition.getJsonId(), edge.getId());
        createdEntities.put(edge.getName(), new CreatedEntityInfo(edge.getName(), "Edge", StringUtils.isEmpty(definition.getCustomer()) ? "Tenant" : definition.getCustomer()));
    }

    public void put(EntitySearchKey entitySearchKey, EntityId entityId) {
        entityIdMap.put(entitySearchKey, entityId);
    }

    public void putIdToMap(EntityDefinition entityDefinition, EntityId entityId) {
        putIdToMap(entityDefinition.getEntityType(), entityDefinition.getName(), entityId);
    }

    public void putIdToMap(EntityType entityType, String entityName, EntityId entityId) {
        putIdToMap(tenantId, entityType, entityName, entityId);
    }

    public void putIdToMap(EntityId ownerId, EntityType entityType, String entityName, EntityId entityId) {
        entityIdMap.put(new EntitySearchKey(ownerId, entityType, entityName, EntityType.ENTITY_GROUP.equals(entityId.getEntityType())), entityId);
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityId> T getIdFromMap(EntityType entityType, String entityName) {
        return (T) entityIdMap.get(new EntitySearchKey(tenantId, entityType, entityName, false));
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityId> T getGroupIdFromMap(EntityType entityType, String entityName) {
        return getGroupIdFromMap(tenantId, entityType, entityName);
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityId> T getGroupIdFromMap(EntityId ownerId, EntityType entityType, String entityName) {
        return (T) entityIdMap.get(new EntitySearchKey(ownerId, entityType, entityName, true));
    }

    public void put(EntityId entityId, List<RelationDefinition> relations) {
        relationDefinitions.put(entityId, relations);
    }

    public void addDeviceCredentials(DeviceCredentialsInfo deviceCredentialsInfo) {
        createdDevices.put(deviceCredentialsInfo.getName(), deviceCredentialsInfo);
    }

    public void addUserCredentials(UserCredentialsInfo userCredentialsInfo) {
        createdUsers.put(userCredentialsInfo.getName(), userCredentialsInfo);
    }

}
