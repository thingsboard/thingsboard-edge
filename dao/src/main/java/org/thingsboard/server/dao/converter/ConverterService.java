/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterSearchQuery;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.Optional;

public interface ConverterService {

    Converter findConverterById(ConverterId converterId);

    ListenableFuture<Converter> findConverterByIdAsync(ConverterId converterId);

    Optional<Converter> findConverterByTenantIdAndName(TenantId tenantId, String name);

    Converter saveConverter(Converter converter);

    void deleteConverter(ConverterId converterId);

    TextPageData<Converter> findConvertersByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<Converter> findConvertersByTenantIdAndType(TenantId tenantId, ConverterType type, TextPageLink pageLink);

    ListenableFuture<List<Converter>> findConvertersByTenantIdAndIdsAsync(TenantId tenantId, List<ConverterId> converterIds);

    void deleteConvertersByTenantId(TenantId tenantId);

    ListenableFuture<List<Converter>> findConvertersByQuery(ConverterSearchQuery query);

    ListenableFuture<List<EntitySubtype>> findConverterTypesByTenantId(TenantId tenantId);

    EntityView findGroupConverter(EntityGroupId entityGroupId, EntityId entityId);

    ListenableFuture<TimePageData<EntityView>> findConvertersByEntityGroupId(EntityGroupId entityGroupId, TimePageLink pageLink);
}
