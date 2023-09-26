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
package org.thingsboard.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableCustomerEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface CustomerDao.
 */
public interface CustomerDao extends Dao<Customer>, TenantEntityDao, ExportableCustomerEntityDao<Customer, CustomerId> {

    /**
     * Save or update customer object
     *
     * @param customer the customer object
     * @return saved customer object
     */
    Customer save(TenantId tenantId, Customer customer);

    /**
     * Find customers by tenant id and page link.
     *
     * @param tenantId the tenant id
     * @param pageLink the page link
     * @return the list of customer objects
     */
    PageData<Customer> findCustomersByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find customers by tenantId and customer title.
     *
     * @param tenantId the tenantId
     * @param title the customer title
     * @return the optional customer object
     */
    Optional<Customer> findCustomersByTenantIdAndTitle(UUID tenantId, String title);

    /**
     * Find customers by tenantId and customer Ids.
     *
     * @param tenantId the tenantId
     * @param customerIds the customer Ids
     * @return the list of customer objects
     */
    ListenableFuture<List<Customer>> findCustomersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> customerIds);

    PageData<Customer> findCustomersByEntityGroupId(UUID groupId, PageLink pageLink);

    PageData<Customer> findCustomersByEntityGroupIds(List<UUID> groupIds, List<UUID> additionalCustomerIds, PageLink pageLink);

}
