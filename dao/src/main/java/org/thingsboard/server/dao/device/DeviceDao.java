/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface DeviceDao.
 *
 */
public interface DeviceDao extends Dao<Device> {

    /**
     * Save or update device object
     *
     * @param device the device object
     * @return saved device object
     */
    Device save(Device device);

    /**
     * Find devices by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of device objects
     */
    List<Device> findDevicesByTenantId(UUID tenantId, TextPageLink pageLink);

    /**
     * Find devices by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    List<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink);

    /**
     * Find devices by tenantId and devices Ids.
     *
     * @param tenantId the tenantId
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds);

    /**
     * Find devices by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of device objects
     */
    List<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink);

    /**
     * Find devices by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of device objects
     */
    List<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink);


    /**
     * Find devices by tenantId, customerId and devices Ids.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param deviceIds the device Ids
     * @return the list of device objects
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    /**
     * Find devices by tenantId and device name.
     *
     * @param tenantId the tenantId
     * @param name the device name
     * @return the optional device object
     */
    Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find tenants device types.
     *
     * @return the list of tenant device type objects
     */
    ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId);
}
