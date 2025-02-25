/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.cf;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface CalculatedFieldService extends EntityDaoService {

    CalculatedField save(CalculatedField calculatedField);

    CalculatedField findById(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    ListenableFuture<CalculatedField> findCalculatedFieldByIdAsync(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findAllCalculatedFields();

    PageData<CalculatedField> findAllCalculatedFields(PageLink pageLink);

    PageData<CalculatedField> findAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    int deleteAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    CalculatedFieldLink saveCalculatedFieldLink(TenantId tenantId, CalculatedFieldLink calculatedFieldLink);

    CalculatedFieldLink findCalculatedFieldLinkById(TenantId tenantId, CalculatedFieldLinkId calculatedFieldLinkId);

    ListenableFuture<CalculatedFieldLink> findCalculatedFieldLinkByIdAsync(TenantId tenantId, CalculatedFieldLinkId calculatedFieldLinkId);

    List<CalculatedFieldLink> findAllCalculatedFieldLinks();

    List<CalculatedFieldLink> findAllCalculatedFieldLinksById(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldLink> findAllCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId);

    ListenableFuture<List<CalculatedFieldLink>> findAllCalculatedFieldLinksByIdAsync(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    PageData<CalculatedFieldLink> findAllCalculatedFieldLinks(PageLink pageLink);

    boolean referencedInAnyCalculatedField(TenantId tenantId, EntityId referencedEntityId);

    boolean referencedInAnyCalculatedFieldIncludingEntityId(TenantId tenantId, EntityId referencedEntityId);

    boolean existsCalculatedFieldByEntityId(TenantId tenantId, EntityId entityId);

}
