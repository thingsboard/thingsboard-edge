/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.translation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.customtranslation.CustomTranslation;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserSettingsEvictEvent;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseCustomTranslationService extends AbstractCachedService<CustomTranslationCompositeKey, CustomTranslation, CustomTranslationEvictEvent> implements CustomTranslationService {

    private final ApplicationEventPublisher eventPublisher;
    private final CustomTranslationDao customTranslationDao;

    @Override
    public CustomTranslation getSystemCustomTranslation(String localeCode) {
        var key =  new CustomTranslationCompositeKey(TenantId.SYS_TENANT_ID, null, localeCode);
        return cache.getAndPutInTransaction(key,
                () -> customTranslationDao.findById(TenantId.SYS_TENANT_ID, key), true);
    }

    @Override
    public CustomTranslation getTenantCustomTranslation(TenantId tenantId, String localeCode) {
        var key =  new CustomTranslationCompositeKey(tenantId, null, localeCode);
        return cache.getAndPutInTransaction(key,
                () -> customTranslationDao.findById(TenantId.SYS_TENANT_ID, key), true);
    }

    @Override
    public CustomTranslation getCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        var key =  new CustomTranslationCompositeKey(tenantId, customerId, localeCode);
        return cache.getAndPutInTransaction(key,
                () -> customTranslationDao.findById(TenantId.SYS_TENANT_ID, key), true);    }

    @Override
    public CustomTranslation getMergedTenantCustomTranslation(TenantId tenantId, String localeCode) {
        CustomTranslation result = getTenantCustomTranslation(tenantId, localeCode);
        result.merge(getSystemCustomTranslation(localeCode));
        return result;
    }

    @Override
    public CustomTranslation getMergedCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        CustomTranslation result = getCustomerCustomTranslation(tenantId, customerId, localeCode);
        result.merge(getTenantCustomTranslation(tenantId, localeCode)).merge(getSystemCustomTranslation(localeCode));
        return result;
    }

    @Override
    public CustomTranslation saveSystemCustomTranslation(CustomTranslation customTranslation) {
        customTranslation.setTenantId(TenantId.SYS_TENANT_ID);
        customTranslationDao.save(TenantId.SYS_TENANT_ID, customTranslation);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(TenantId.SYS_TENANT_ID).entityId(TenantId.SYS_TENANT_ID)
                .edgeEventType(EdgeEventType.CUSTOM_TRANSLATION).actionType(ActionType.UPDATED).build());
        return getSystemCustomTranslation(customTranslation.getLocaleCode());
    }

    @Override
    public CustomTranslation saveTenantCustomTranslation(TenantId tenantId, CustomTranslation customTranslation) {
        customTranslation.setTenantId(tenantId);
        customTranslationDao.save(tenantId, customTranslation);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(tenantId)
                .edgeEventType(EdgeEventType.CUSTOM_TRANSLATION).actionType(ActionType.UPDATED).build());
        return getTenantCustomTranslation(tenantId, customTranslation.getLocaleCode());
    }

    @Override
    public CustomTranslation saveCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, CustomTranslation customTranslation) {
        customTranslation.setTenantId(tenantId);
        customTranslation.setCustomerId(customerId);
        customTranslationDao.save(tenantId, customTranslation);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(customerId)
                .edgeEventType(EdgeEventType.CUSTOM_TRANSLATION).actionType(ActionType.UPDATED).build());
        return getCustomerCustomTranslation(tenantId, customerId, customTranslation.getLocaleCode());
    }

    @Override
    public List<String> getLocales(TenantId tenantId, CustomerId customerId) {
        return customTranslationDao.findAllLocalesByTenantIdAndCustomerId(tenantId, customerId);
    }

    @TransactionalEventListener(classes = CustomTranslationEvictEvent.class)
    @Override
    public void handleEvictEvent(CustomTranslationEvictEvent event) {
        cache.evict(event.getKey());
    }
}
