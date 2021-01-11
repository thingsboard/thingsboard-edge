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
package org.thingsboard.server.dao.blob;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface BlobEntityInfoDao.
 *
 */
public interface BlobEntityInfoDao extends Dao<BlobEntityInfo> {

    BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(UUID tenantId, UUID blobEntityId);

    /**
     * Find blob entities by tenantId.
     *
     * @param tenantId the tenantId
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(UUID tenantId, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId and type.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndType(UUID tenantId, String type, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId and customerId.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId, customerId and type.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the pageLink
     * @return the list of blob entity objects
     */
    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TimePageLink pageLink);

    /**
     * Find blob entities by tenantId and blob entity Ids.
     *
     * @param tenantId the tenantId
     * @param blobEntityIds the blob entity Ids
     * @return the list of blob entity objects
     */
    ListenableFuture<List<BlobEntityInfo>> findBlobEntitiesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> blobEntityIds);

}
