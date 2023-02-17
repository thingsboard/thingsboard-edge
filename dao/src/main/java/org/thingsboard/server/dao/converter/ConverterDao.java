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
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface ConverterDao.
 *
 */
public interface ConverterDao extends Dao<Converter>, TenantEntityDao, ExportableEntityDao<ConverterId, Converter> {

    /**
     * Find all (core and edge template) converters by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of converter objects
     */
    PageData<Converter> findByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find core converters by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of converter objects
     */
    PageData<Converter> findCoreConvertersByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find edge template converters by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of converter objects
     */
    PageData<Converter> findEdgeTemplateConvertersByTenantId(UUID tenantId, PageLink pageLink);

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
