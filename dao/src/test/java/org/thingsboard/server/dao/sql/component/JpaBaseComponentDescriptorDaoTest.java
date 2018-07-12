/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.component;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.component.ComponentDescriptorDao;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaBaseComponentDescriptorDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ComponentDescriptorDao componentDescriptorDao;

    @Test
    public void findByType() {
        for (int i = 0; i < 20; i++) {
            createComponentDescriptor(ComponentType.FILTER, ComponentScope.SYSTEM, i);
            createComponentDescriptor(ComponentType.ACTION, ComponentScope.TENANT, i + 20);
        }

        TextPageLink pageLink1 = new TextPageLink(15, "COMPONENT_");
        List<ComponentDescriptor> components1 = componentDescriptorDao.findByTypeAndPageLink(ComponentType.FILTER, pageLink1);
        assertEquals(15, components1.size());

        TextPageLink pageLink2 = new TextPageLink(15, "COMPONENT_", components1.get(14).getId().getId(), null);
        List<ComponentDescriptor> components2 = componentDescriptorDao.findByTypeAndPageLink(ComponentType.FILTER, pageLink2);
        assertEquals(5, components2.size());
    }

    @Test
    public void findByTypeAndSocpe() {
        for (int i = 0; i < 20; i++) {
            createComponentDescriptor(ComponentType.ENRICHMENT, ComponentScope.SYSTEM, i);
            createComponentDescriptor(ComponentType.ACTION, ComponentScope.TENANT, i + 20);
            createComponentDescriptor(ComponentType.FILTER, ComponentScope.SYSTEM, i + 40);
        }

        TextPageLink pageLink1 = new TextPageLink(15, "COMPONENT_");
        List<ComponentDescriptor> components1 = componentDescriptorDao.findByScopeAndTypeAndPageLink(
                ComponentScope.SYSTEM, ComponentType.FILTER, pageLink1);
        assertEquals(15, components1.size());

        TextPageLink pageLink2 = new TextPageLink(15, "COMPONENT_", components1.get(14).getId().getId(), null);
        List<ComponentDescriptor> components2 = componentDescriptorDao.findByScopeAndTypeAndPageLink(
                ComponentScope.SYSTEM, ComponentType.FILTER, pageLink2);
        assertEquals(5, components2.size());
    }

    private void createComponentDescriptor(ComponentType type, ComponentScope scope, int index) {
        ComponentDescriptor component = new ComponentDescriptor();
        component.setId(new ComponentDescriptorId(UUIDs.timeBased()));
        component.setType(type);
        component.setScope(scope);
        component.setName("COMPONENT_" + index);
        componentDescriptorDao.save(component);
    }

}
