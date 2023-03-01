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
package org.thingsboard.server.dao.wl;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

public interface WhiteLabelingService {

    String EDGE_LOGIN_WHITE_LABEL_DOMAIN_NAME = "edge.domain.name";

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

    // TODO: @voba - methods added on edge for login whitelabeling
    AdminSettings saveOrUpdateEdgeLoginWhiteLabelSettings(TenantId tenantId, EntityId currentEntityId);
}
