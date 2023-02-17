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
package org.thingsboard.server.dao.sql.widget;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    final String BUNDLE_ALIAS = "BUNDLE_ALIAS";
    final int WIDGET_TYPE_COUNT = 3;
    List<WidgetType> widgetTypeList;

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Before
    public void setUp() {
        widgetTypeList = new ArrayList<>();
        for (int i = 0; i < WIDGET_TYPE_COUNT; i++) {
            widgetTypeList.add(createAndSaveWidgetType(i));
        }
    }

    WidgetType createAndSaveWidgetType(int number) {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setTenantId(TenantId.SYS_TENANT_ID);
        widgetType.setName("WIDGET_TYPE_" + number);
        widgetType.setAlias("ALIAS_" + number);
        widgetType.setBundleAlias(BUNDLE_ALIAS);
        return widgetTypeDao.save(TenantId.SYS_TENANT_ID, widgetType);
    }

    @After
    public void deleteAllWidgetType() {
        for (WidgetType widgetType : widgetTypeList) {
            widgetTypeDao.removeById(TenantId.SYS_TENANT_ID, widgetType.getUuidId());
        }
    }

    @Test
    public void testFindByTenantIdAndBundleAlias() {
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(TenantId.SYS_TENANT_ID.getId(), BUNDLE_ALIAS);
        assertEquals(WIDGET_TYPE_COUNT, widgetTypes.size());
    }

    @Test
    public void testFindByTenantIdAndBundleAliasAndAlias() {
        WidgetType result = widgetTypeList.get(0);
        assertNotNull(result);
        WidgetType widgetType = widgetTypeDao.findByTenantIdBundleAliasAndAlias(TenantId.SYS_TENANT_ID.getId(), BUNDLE_ALIAS, "ALIAS_0");
        assertEquals(result.getId(), widgetType.getId());
    }
}
