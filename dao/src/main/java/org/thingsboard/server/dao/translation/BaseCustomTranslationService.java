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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.entity.AbstractCachedService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;

import java.util.List;

import static org.thingsboard.common.util.JacksonUtil.merge;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseCustomTranslationService extends AbstractCachedService<CustomTranslationCompositeKey, CustomTranslation, CustomTranslationEvictEvent> implements CustomTranslationService {

    private final CustomerService customerService;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomTranslationDao customTranslationDao;

    @Override
    public CustomTranslation getCurrentCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        CustomTranslationCompositeKey key = new CustomTranslationCompositeKey(tenantId, customerId, localeCode);
        CustomTranslation customTranslation = cache.getAndPutInTransaction(key,
                () -> customTranslationDao.findById(TenantId.SYS_TENANT_ID, key), true);
        if (customTranslation == null){
            customTranslation = CustomTranslation.builder()
                    .localeCode(key.getLocaleCode())
                    .value(JacksonUtil.newObjectNode()).build();
        }
        return customTranslation;
    }

    @Override
    public JsonNode getMergedTenantCustomTranslation(TenantId tenantId, String localeCode) {
        JsonNode tenantCustomTranslation = getCurrentCustomTranslation(tenantId, null,  localeCode).getValue().deepCopy();
        JsonNode systemCustomTranslation = getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode).getValue();
        return merge(tenantCustomTranslation, systemCustomTranslation);
    }

    @Override
    public JsonNode getMergedCustomerCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        JsonNode customerCustomTranslation = getCurrentCustomTranslation(tenantId, customerId, localeCode).getValue().deepCopy();
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            customerCustomTranslation = getMergedCustomerHierarchyCustomTranslation(tenantId, customer.getParentCustomerId(), localeCode, customerCustomTranslation);
        }
        JsonNode tenantCustomTranslation = getCurrentCustomTranslation(tenantId, null, localeCode).getValue();
        JsonNode systemCustomTranslation = getCurrentCustomTranslation(TenantId.SYS_TENANT_ID, null, localeCode).getValue();

        return merge(merge(customerCustomTranslation, tenantCustomTranslation), systemCustomTranslation);
    }

    @Override
    public CustomTranslation saveCustomTranslation(CustomTranslation customTranslation) {
        customTranslationDao.save(customTranslation.getTenantId(), customTranslation);
        publishEvictEvent(new CustomTranslationEvictEvent(new CustomTranslationCompositeKey(customTranslation.getTenantId(), customTranslation.getCustomerId(), customTranslation.getLocaleCode())));
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(TenantId.SYS_TENANT_ID).entityId(TenantId.SYS_TENANT_ID)
                .edgeEventType(EdgeEventType.CUSTOM_TRANSLATION).actionType(ActionType.UPDATED).build());
        return getCurrentCustomTranslation(customTranslation.getTenantId(), customTranslation.getCustomerId(), customTranslation.getLocaleCode());
    }

    @Override
    public CustomTranslation patchCustomTranslation(CustomTranslation newCustomTranslation) {
        CustomTranslation customTranslation = getCurrentCustomTranslation(newCustomTranslation.getTenantId(), newCustomTranslation.getCustomerId(), newCustomTranslation.getLocaleCode());
        JacksonUtil.update(customTranslation.getValue(), newCustomTranslation.getValue());
        return saveCustomTranslation(customTranslation);
    }

    @Override
    public CustomTranslation deleteCustomTranslationKey(TenantId tenantId, CustomerId customerId, String localeCode, String key) {
        CustomTranslation customTranslation = getCurrentCustomTranslation(tenantId, customerId, localeCode);
        JacksonUtil.deleteKey(customTranslation.getValue(), key);
        return saveCustomTranslation(customTranslation);
    }

    @Override
    public void deleteCustomTranslation(TenantId tenantId, CustomerId customerId, String localeCode) {
        CustomTranslationCompositeKey key = new CustomTranslationCompositeKey(tenantId, customerId, localeCode);
        customTranslationDao.removeById(tenantId, key);
        publishEvictEvent(new CustomTranslationEvictEvent(new CustomTranslationCompositeKey(tenantId, customerId, localeCode)));
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(TenantId.SYS_TENANT_ID).entityId(TenantId.SYS_TENANT_ID)
                .edgeEventType(EdgeEventType.CUSTOM_TRANSLATION).actionType(ActionType.UPDATED).build());
    }

    @Override
    public List<String> getCustomizedLocales(TenantId tenantId, CustomerId customerId) {
        return customTranslationDao.findLocalesByTenantIdAndCustomerId(tenantId, customerId);
    }

    private JsonNode getMergedCustomerHierarchyCustomTranslation(TenantId tenantId, CustomerId customerId, String locale, JsonNode customTranslation) {
        CustomTranslation parentCustomerCustomTranslation = getCurrentCustomTranslation(tenantId, customerId, locale);
        JsonNode merged = merge(customTranslation, parentCustomerCustomTranslation.getValue());
        Customer customer = customerService.findCustomerById(tenantId, customerId);
        if (customer.isSubCustomer()) {
            return getMergedCustomerHierarchyCustomTranslation(tenantId, customer.getParentCustomerId(), locale, merged);
        } else {
            return merged;
        }
    }

    @TransactionalEventListener(classes = CustomTranslationEvictEvent.class)
    @Override
    public void handleEvictEvent(CustomTranslationEvictEvent event) {
        cache.evict(event.getKey());
    }

}
