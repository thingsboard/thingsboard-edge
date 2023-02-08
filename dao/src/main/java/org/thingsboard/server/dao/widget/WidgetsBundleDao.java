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
package org.thingsboard.server.dao.widget;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface WidgetsBundleDao.
 */
public interface WidgetsBundleDao extends Dao<WidgetsBundle>, ExportableEntityDao<WidgetsBundleId, WidgetsBundle> {

    /**
     * Save or update widgets bundle object
     *
     * @param tenantId the tenantId
     * @param widgetsBundle the widgets bundle object
     * @return saved widgets bundle object
     */
    WidgetsBundle save(TenantId tenantId, WidgetsBundle widgetsBundle);

    /**
     * Find widgets bundle by tenantId and alias.
     *
     * @param tenantId the tenantId
     * @param alias the alias
     * @return the widgets bundle object
     */
    WidgetsBundle findWidgetsBundleByTenantIdAndAlias(UUID tenantId, String alias);

    /**
     * Find system widgets bundles by page link.
     *
     * @param pageLink the page link
     * @return the list of widgets bundles objects
     */
    PageData<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId, PageLink pageLink);

    /**
     * Find tenant widgets bundles by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of widgets bundles objects
     */
    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find all tenant widgets bundles (including system) by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of widgets bundles objects
     */
    PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(UUID tenantId, PageLink pageLink);

    ListenableFuture<List<WidgetsBundle>> findSystemWidgetBundlesByIdsAsync(UUID tenantId, List<UUID> widgetsBundleIds);

    ListenableFuture<List<WidgetsBundle>> findAllTenantWidgetBundlesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> widgetsBundleIds);
}

