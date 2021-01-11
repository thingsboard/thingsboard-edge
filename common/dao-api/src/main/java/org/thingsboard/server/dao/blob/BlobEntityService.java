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
package org.thingsboard.server.dao.blob;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;

public interface BlobEntityService {

    BlobEntity findBlobEntityById(TenantId tenantId, BlobEntityId blobEntityId);

    BlobEntityInfo findBlobEntityInfoById(TenantId tenantId, BlobEntityId blobEntityId);

    BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(TenantId tenantId, BlobEntityId blobEntityId);

    ListenableFuture<BlobEntityInfo> findBlobEntityInfoByIdAsync(TenantId tenantId, BlobEntityId blobEntityId);

    ListenableFuture<List<BlobEntityInfo>> findBlobEntityInfoByIdsAsync(TenantId tenantId, List<BlobEntityId> blobEntityIds);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(TenantId tenantId, TimePageLink pageLink);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndType(TenantId tenantId, String type, TimePageLink pageLink);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TimePageLink pageLink);

    PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TimePageLink pageLink);

    BlobEntity saveBlobEntity(BlobEntity blobEntity);

    void deleteBlobEntity(TenantId tenantId, BlobEntityId blobEntityId);

    void deleteBlobEntitiesByTenantId(TenantId tenantId);

    void deleteBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

}
