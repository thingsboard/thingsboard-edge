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
package org.thingsboard.server.dao.sql.entityview;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.entityview.EntityViewInfoDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JpaEntityViewInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private EntityViewInfoDao entityViewInfoDao;

    @Autowired
    private EntityViewDao entityViewDao;

    @Autowired
    private CustomerDao customerDao;

    private List<EntityView> entityViews = new ArrayList<>();

    @After
    public void tearDown() {
        for (EntityView entityView : entityViews) {
            entityViewDao.removeById(entityView.getTenantId(), entityView.getUuidId());
        }
        entityViews.clear();
    }

    @Test
    public void testFindEntityViewInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            entityViews.add(createEntityView(tenantId1, null, i));
            entityViews.add(createEntityView(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "ENTITY_VIEW");
        PageData<EntityViewInfo> entityViewInfos1 = entityViewInfoDao.findEntityViewsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, entityViewInfos1.getData().size());

        PageData<EntityViewInfo> entityViewInfos2 = entityViewInfoDao.findEntityViewsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, entityViewInfos2.getData().size());
    }

    @Test
    public void testFindEntityViewInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            entityViews.add(createEntityView(tenantId1, customer1.getUuidId(), i));
            entityViews.add(createEntityView(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "ENTITY_VIEW", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<EntityViewInfo> entityViewInfos1 = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, entityViewInfos1.getData().size());
        entityViewInfos1.getData().forEach(entityViewInfo -> Assert.assertNotEquals("CUSTOMER_0", entityViewInfo.getOwnerName()));

        PageData<EntityViewInfo> entityViewInfos2 = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, entityViewInfos2.getData().size());

        PageData<EntityViewInfo> entityViewInfos3 = entityViewInfoDao.findEntityViewsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, entityViewInfos3.getData().size());
    }

    private EntityView createEntityView(UUID tenantId, UUID customerId, int index) {
        return this.createEntityView(tenantId, customerId, null, index);
    }

    private EntityView createEntityView(UUID tenantId, UUID customerId, String type, int index) {
        if (type == null) {
            type = "default";
        }
        EntityView entityView = new EntityView();
        entityView.setId(new EntityViewId(Uuids.timeBased()));
        entityView.setTenantId(TenantId.fromUUID(tenantId));
        entityView.setCustomerId(new CustomerId(customerId));
        entityView.setName("ENTITY_VIEW_" + index);
        entityView.setType(type);
        entityView.setEntityId(new DeviceId(Uuids.timeBased()));
        return entityViewDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, entityView);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

}
