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
package org.thingsboard.server.dao.translation;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;

public interface CustomTranslationService {

    CustomTranslation getSystemCustomTranslation(TenantId tenantId);

    CustomTranslation getTenantCustomTranslation(TenantId tenantId);

    CustomTranslation getCustomerCustomTranslation(TenantId tenantId, CustomerId customerId);

    CustomTranslation getMergedTenantCustomTranslation(TenantId tenantId);

    CustomTranslation getMergedCustomerCustomTranslation(TenantId tenantId, CustomerId customerId);

    CustomTranslation saveSystemCustomTranslation(CustomTranslation customTranslation);

    CustomTranslation saveTenantCustomTranslation(TenantId tenantId, CustomTranslation customTranslation);

    CustomTranslation saveCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, CustomTranslation customTranslation);

}
