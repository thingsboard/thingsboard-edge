/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.role.RoleService;

import java.util.Set;

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbPeContext {

    IntegrationService getIntegrationService();

    EntityGroupService getEntityGroupService();

    ReportService getReportService();

    BlobEntityService getBlobEntityService();

    GroupPermissionService getGroupPermissionService();

    RoleService getRoleService();

    EntityId getOwner(TenantId tenantId, EntityId entityId);

    void clearOwners(EntityId entityId);

    Set<EntityId> getChildOwners(TenantId tenantId, EntityId parentOwnerId);

    void changeDashboardOwner(TenantId tenantId, EntityId targetOwnerId, Dashboard dashboard) throws ThingsboardException;

    void changeUserOwner(TenantId tenantId, EntityId targetOwnerId, User user) throws ThingsboardException;

    void changeCustomerOwner(TenantId tenantId, EntityId targetOwnerId, Customer customer) throws ThingsboardException;

    void changeEntityViewOwner(TenantId tenantId, EntityId targetOwnerId, EntityView entityView) throws ThingsboardException;

    void changeAssetOwner(TenantId tenantId, EntityId targetOwnerId, Asset asset) throws ThingsboardException;

    void changeDeviceOwner(TenantId tenantId, EntityId targetOwnerId, Device device) throws ThingsboardException;

    void changeEntityOwner(TenantId tenantId, EntityId targetOwnerId, EntityId entityId, EntityType entityType) throws ThingsboardException;

    void pushToIntegration(IntegrationId integrationId, TbMsg tbMsg, FutureCallback<Void> callback);

    void ack(TbMsg msg);

    boolean isLocalEntity(EntityId entityId);

    ScriptEngine createAttributesJsScriptEngine(String script);

}
