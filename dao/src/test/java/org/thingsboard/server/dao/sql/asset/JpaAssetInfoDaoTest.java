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
package org.thingsboard.server.dao.sql.asset;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetInfoDao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JpaAssetInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AssetInfoDao assetInfoDao;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetProfileDao assetProfileDao;

    @Autowired
    private CustomerDao customerDao;

    private List<Asset> assets = new ArrayList<>();

    private Map<String, AssetProfileId> savedAssetProfiles = new HashMap<>();

    @After
    public void tearDown() {
        for (Asset asset : assets) {
            assetDao.removeById(asset.getTenantId(), asset.getUuidId());
        }
        assets.clear();
        for (AssetProfileId assetProfileId : savedAssetProfiles.values()) {
            assetProfileDao.removeById(TenantId.SYS_TENANT_ID, assetProfileId.getId());
        }
        savedAssetProfiles.clear();
    }

    @Test
    public void testFindAssetInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            assets.add(createAsset(tenantId1, null, i));
            assets.add(createAsset(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "ASSET");
        PageData<AssetInfo> assetInfos1 = assetInfoDao.findAssetsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, assetInfos1.getData().size());

        PageData<AssetInfo> assetInfos2 = assetInfoDao.findAssetsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, assetInfos2.getData().size());
    }

    @Test
    public void testFindAssetInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            assets.add(createAsset(tenantId1, customer1.getUuidId(), i));
            assets.add(createAsset(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "ASSET", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<AssetInfo> assetInfos1 = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, assetInfos1.getData().size());
        assetInfos1.getData().forEach(assetInfo -> Assert.assertNotEquals("CUSTOMER_0", assetInfo.getOwnerName()));

        PageData<AssetInfo> assetInfos2 = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, assetInfos2.getData().size());

        PageData<AssetInfo> assetInfos3 = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, assetInfos3.getData().size());
    }

    private Asset createAsset(UUID tenantId, UUID customerId, int index) {
        return this.createAsset(tenantId, customerId, null, index);
    }

    private Asset createAsset(UUID tenantId, UUID customerId, String type, int index) {
        if (type == null) {
            type = "default";
        }
        Asset asset = new Asset();
        asset.setId(new AssetId(Uuids.timeBased()));
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setCustomerId(new CustomerId(customerId));
        asset.setName("ASSET_" + index);
        asset.setType(type);
        asset.setAssetProfileId(assetProfileId(type));
        return assetDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, asset);
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

    private AssetProfileId assetProfileId(String type) {
        AssetProfileId assetProfileId = savedAssetProfiles.get(type);
        if (assetProfileId == null) {
            AssetProfile assetProfile = new AssetProfile();
            assetProfile.setName(type);
            assetProfile.setTenantId(TenantId.SYS_TENANT_ID);
            assetProfile.setDescription("Test");
            AssetProfile savedAssetProfile = assetProfileDao.save(TenantId.SYS_TENANT_ID, assetProfile);
            assetProfileId = savedAssetProfile.getId();
            savedAssetProfiles.put(type, assetProfileId);
        }
        return assetProfileId;
    }

}
