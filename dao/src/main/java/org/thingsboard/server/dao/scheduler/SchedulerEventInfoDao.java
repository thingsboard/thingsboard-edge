/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.scheduler;

import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface SchedulerEventInfoDao.
 *
 */
public interface SchedulerEventInfoDao extends Dao<SchedulerEventInfo> {

    /**
     * Find scheduler events by tenantId.
     *
     * @param tenantId the tenantId
     * @return the list of scheduler event objects
     */
    List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId);

    /**
     * Find scheduler events by tenantId and type.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @return the list of scheduler event objects
     */
    List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndType(UUID tenantId, String type);

    /**
     * Find scheduler events by tenantId and customerId.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @return the list of scheduler event objects
     */
    List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    /**
     * Find scheduler events by tenantId, customerId and type.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @return the list of scheduler event objects
     */
    List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type);

}
