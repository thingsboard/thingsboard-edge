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
package org.thingsboard.server.dao.entity;

import com.google.common.collect.MapMaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultEntityServiceRegistry implements EntityServiceRegistry {

    private final ApplicationContext applicationContext;
    private final Map<EntityType, EntityDaoService> entityDaoServicesMap = new MapMaker().weakValues().makeMap();

    @EventListener(ContextRefreshedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void init() {
        log.debug("Initializing EntityServiceRegistry on ContextRefreshedEvent");
        applicationContext.getBeansOfType(EntityDaoService.class).values().forEach(entityDaoService -> {
            EntityType entityType = entityDaoService.getEntityType();
            entityDaoServicesMap.put(entityType, entityDaoService);
            if (EntityType.RULE_CHAIN.equals(entityType)) {
                entityDaoServicesMap.put(EntityType.RULE_NODE, entityDaoService);
            }
        });
        log.debug("Initialized EntityServiceRegistry total [{}] entries", entityDaoServicesMap.size());
    }

    @Override
    public EntityDaoService getServiceByEntityType(EntityType entityType) {
        return entityDaoServicesMap.get(entityType);
    }

}
