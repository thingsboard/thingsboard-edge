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
package org.thingsboard.server.dao.wl;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

import java.util.concurrent.ExecutionException;

public interface WhiteLabelingService {

    WhiteLabelingParams getSystemWhiteLabelingParams(TenantId tenantId);

    LoginWhiteLabelingParams getSystemLoginWhiteLabelingParams(TenantId tenantId);

    ListenableFuture<WhiteLabelingParams> getTenantWhiteLabelingParams(TenantId tenantId);

    ListenableFuture<WhiteLabelingParams> getCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId);

    WhiteLabelingParams getMergedSystemWhiteLabelingParams(TenantId tenantId, String logoImageChecksum, String faviconChecksum);

    WhiteLabelingParams getMergedTenantWhiteLabelingParams(TenantId tenantId, String logoImageChecksum, String faviconChecksum) throws Exception;

    WhiteLabelingParams getMergedCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, String logoImageChecksum, String faviconChecksum) throws Exception;

    LoginWhiteLabelingParams getTenantLoginWhiteLabelingParams(TenantId tenantId) throws Exception;

    LoginWhiteLabelingParams getCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId) throws Exception;

    LoginWhiteLabelingParams getMergedLoginWhiteLabelingParams(TenantId tenantId, String domainName, String logoImageChecksum, String faviconChecksum) throws Exception;

    WhiteLabelingParams saveSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams);

    ListenableFuture<WhiteLabelingParams> saveTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams);

    ListenableFuture<WhiteLabelingParams> saveCustomerWhiteLabelingParams(TenantId tenantId, CustomerId customerId, WhiteLabelingParams whiteLabelingParams);

    LoginWhiteLabelingParams saveSystemLoginWhiteLabelingParams(LoginWhiteLabelingParams loginWhiteLabelingParams);

    LoginWhiteLabelingParams saveTenantLoginWhiteLabelingParams(TenantId tenantId, LoginWhiteLabelingParams loginWhiteLabelingParams) throws Exception;

    LoginWhiteLabelingParams saveCustomerLoginWhiteLabelingParams(TenantId tenantId, CustomerId customerId, LoginWhiteLabelingParams loginWhiteLabelingParams) throws Exception;

    WhiteLabelingParams mergeSystemWhiteLabelingParams(WhiteLabelingParams whiteLabelingParams);

    WhiteLabelingParams mergeTenantWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams);

    WhiteLabelingParams mergeCustomerWhiteLabelingParams(TenantId tenantId, WhiteLabelingParams whiteLabelingParams);

    void deleteDomainWhiteLabelingByEntityId(TenantId tenantId, EntityId entityId);

    boolean isWhiteLabelingAllowed(TenantId tenantId, EntityId entityId);

    boolean isCustomerWhiteLabelingAllowed(TenantId tenantId);

}
