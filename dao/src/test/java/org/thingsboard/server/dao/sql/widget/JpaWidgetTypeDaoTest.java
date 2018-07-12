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
package org.thingsboard.server.dao.sql.widget;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaWidgetTypeDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Test
    @DatabaseSetup(("classpath:dbunit/widget_type.xml"))
    public void testFindByTenantIdAndBundleAlias() {
        UUID tenantId = UUID.fromString("2b7e4c90-2dfe-11e7-94aa-f7f6dbfb4833");
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId, "BUNDLE_ALIAS_1");
        assertEquals(3, widgetTypes.size());
    }

    @Test
    @DatabaseSetup(("classpath:dbunit/widget_type.xml"))
    public void testFindByTenantIdAndBundleAliasAndAlias() {
        UUID tenantId = UUID.fromString("2b7e4c90-2dfe-11e7-94aa-f7f6dbfb4833");
        WidgetType widgetType = widgetTypeDao.findByTenantIdBundleAliasAndAlias(tenantId, "BUNDLE_ALIAS_1", "ALIAS3");
        UUID id = UUID.fromString("2b7e4c93-2dfe-11e7-94aa-f7f6dbfb4833");
        assertEquals(id, widgetType.getId().getId());
    }
}
