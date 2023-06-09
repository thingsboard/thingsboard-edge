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
package org.thingsboard.server.service.entitiy.converter;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@AllArgsConstructor
public class DefaultTbConverterService extends AbstractTbEntityService implements TbConverterService {

    private final ConverterService converterService;

    @Override
    public Converter save(Converter converter, User user) throws Exception {
        ActionType actionType = converter.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = converter.getTenantId();
        try {
            Converter savedConverter = checkNotNull(converterService.saveConverter(converter));

            autoCommit(user, savedConverter.getId());

            if (!converter.isEdgeTemplate()) {
                tbClusterService.broadcastEntityStateChangeEvent(savedConverter.getTenantId(), savedConverter.getId(),
                        actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            }

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedConverter.getId(), savedConverter,
                    user, actionType, (converter.isEdgeTemplate() && ActionType.UPDATED.equals(actionType)), null);
            return savedConverter;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CONVERTER), converter,
                    actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Converter converter, User user) {
        TenantId tenantId = converter.getTenantId();
        ConverterId converterId = converter.getId();
        try {

            converterService.deleteConverter(tenantId, converterId);

            if (!converter.isEdgeTemplate()) {
                tbClusterService.broadcastEntityStateChangeEvent(tenantId, converterId, ComponentLifecycleEvent.DELETED);
            }

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, converter.getId(), converter,
                    user, ActionType.DELETED, false, null, converterId.getId().toString());

        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CONVERTER),
                    ActionType.DELETED, user, e, converterId.getId().toString());
            throw e;
        }
    }
}
