/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface AccessControlService {

    void checkPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException;

    boolean hasPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException;

    <I extends EntityId, T extends TenantEntity> void checkPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException;

    <I extends EntityId, T extends TenantEntity> void checkPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity, EntityGroupId entityGroupId) throws ThingsboardException;

    <I extends EntityId, T extends TenantEntity> boolean hasPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException;

    void checkEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException;

    boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException;

}
