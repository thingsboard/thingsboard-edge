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
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface ConverterDao.
 *
 */
public interface ConverterDao extends Dao<Converter>, TenantEntityDao {

    /**
     * Find converters by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of converter objects
     */
    PageData<Converter> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find converter by tenantId and converter name.
     *
     * @param tenantId the tenantId
     * @param name     the converter name
     * @return the optional converter object
     */
    Optional<Converter> findConverterByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find converters by tenantId and converter Ids.
     *
     * @param tenantId the tenantId
     * @param converterIds the converter Ids
     * @return the list of converter objects
     */
    ListenableFuture<List<Converter>> findConvertersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> converterIds);

}
