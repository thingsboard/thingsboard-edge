/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import org.apache.commons.text.CaseUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

@Service
public class DefaultEntityServiceRegistry implements EntityServiceRegistry {

    private static final String SERVICE_SUFFIX = "DaoService";

    private Map<String, EntityDaoService> entityDaoServicesMap;

    private final ApplicationContext applicationContext;

    public DefaultEntityServiceRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        entityDaoServicesMap = applicationContext.getBeansOfType(EntityDaoService.class);
    }

    @PreDestroy
    public void destroy() {
        if (entityDaoServicesMap != null) {
            entityDaoServicesMap.clear();
        }
    }

    @Override
    public EntityDaoService getServiceByEntityType(EntityType entityType) {
        String beanName = EntityType.RULE_NODE.equals(entityType) ? getBeanName(EntityType.RULE_CHAIN) : getBeanName(entityType);
        return entityDaoServicesMap.get(beanName);
    }

    private String getBeanName(EntityType entityType) {
        return CaseUtils.toCamelCase(entityType.name(), true, '_') + SERVICE_SUFFIX;
    }

}
