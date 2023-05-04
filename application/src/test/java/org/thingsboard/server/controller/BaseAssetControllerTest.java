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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.stats.DefaultRuleEngineStatisticsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@ContextConfiguration(classes = {BaseAssetControllerTest.Config.class})
@DaoSqlTest
public class BaseAssetControllerTest extends AbstractControllerTest {

    private IdComparator<Asset> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;
    private final  String classNameAsset = "Asset";

    @Autowired
    private AssetDao assetDao;

    static class Config {
        @Bean
        @Primary
        public AssetDao assetDao(AssetDao assetDao) {
            return Mockito.mock(AssetDao.class, AdditionalAnswers.delegatesTo(assetDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAsset.getCustomerId().getId());
        Assert.assertEquals(asset.getName(), savedAsset.getName());

        Mockito.reset(tbClusterService, auditLogService);

        savedAsset.setName("My new asset");
        doPost("/api/asset", savedAsset, Asset.class);

        testNotifyEntityEntityGroupNullAllOneTime(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(foundAsset.getName(), savedAsset.getName());
    }

    @Test
    public void testSaveAssetWithViolationOfLengthValidation() throws Exception {
        Asset asset = new Asset();
        asset.setName(StringUtils.randomAlphabetic(300));
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));
        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        asset.setName("Normal name");
        asset.setType(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("type");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));
        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        asset.setType("default");
        asset.setLabel(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("label");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));
        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateAssetFromDifferentTenant() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = classNameAsset.toUpperCase(Locale.ENGLISH) + " '" + savedAsset.getName() +"'!";
        doPost("/api/asset", savedAsset)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(savedAsset, savedDifferentTenant.getId(), savedDifferentTenantUser.getId(),
                DIFFERENT_TENANT_ADMIN_EMAIL, ActionType.UPDATED,
                new ThingsboardException(msgErrorPermissionWrite + msgError, ThingsboardErrorCode.PERMISSION_DENIED));

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionDelete + msgError)));


        testNotifyEntityNever(savedAsset.getId(), savedAsset);

        deleteDifferentTenant();
    }

    @Test
    public void testSaveAssetWithProfileFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        AssetProfile differentProfile = createAssetProfile("Different profile");
        differentProfile = doPost("/api/assetProfile", differentProfile, AssetProfile.class);

        loginTenantAdmin();
        Asset asset = new Asset();
        asset.setName("My device");
        asset.setAssetProfileId(differentProfile.getId());
        doPost("/api/asset", asset).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Asset can`t be referencing to asset profile from different tenant!")));
    }

    @Test
    public void testFindAssetById() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertNotNull(foundAsset);
        Assert.assertEquals(savedAsset, foundAsset);
    }

    @Test
    public void testFindAssetTypesByTenantId() throws Exception {
        List<Asset> assets = new ArrayList<>();

        Mockito.reset(tbClusterService, auditLogService);

        int cntTime = 3;
        for (int i = 0; i < cntTime; i++) {
            Asset asset = new Asset();
            asset.setName("My asset B" + i);
            asset.setType("typeB");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Asset(), new Asset(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntTime);

        for (int i = 0; i < 7; i++) {
            Asset asset = new Asset();
            asset.setName("My asset C" + i);
            asset.setType("typeC");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        for (int i = 0; i < 9; i++) {
            Asset asset = new Asset();
            asset.setName("My asset A" + i);
            asset.setType("typeA");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        List<EntitySubtype> assetTypes = doGetTyped("/api/asset/types",
                new TypeReference<List<EntitySubtype>>() {
                });

        Assert.assertNotNull(assetTypes);
        Assert.assertEquals(3, assetTypes.size());
        Assert.assertEquals("typeA", assetTypes.get(0).getType());
        Assert.assertEquals("typeB", assetTypes.get(1).getType());
        Assert.assertEquals("typeC", assetTypes.get(2).getType());
    }

    @Test
    public void testDeleteAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedAsset.getId().getId().toString());

        String assetIdStr = savedAsset.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(classNameAsset, assetIdStr))));
    }

    @Test
    public void testDeleteAssetAssignedToEntityView() throws Exception {
        Asset asset1 = new Asset();
        asset1.setName("My asset 1");
        asset1.setType("default");
        Asset savedAsset1 = doPost("/api/asset", asset1, Asset.class);

        Asset asset2 = new Asset();
        asset2.setName("My asset 2");
        asset2.setType("default");
        Asset savedAsset2 = doPost("/api/asset", asset2, Asset.class);

        EntityView view = new EntityView();
        view.setEntityId(savedAsset1.getId());
        view.setTenantId(savedTenant.getId());
        view.setName("My entity view");
        view.setType("default");
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Can't delete asset that has entity views";
        doDelete("/api/asset/" + savedAsset1.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeEdgeServiceNeverError(savedAsset1, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, new DataValidationException(msgError), savedAsset1.getId().getId().toString());

        savedView.setEntityId(savedAsset2.getId());

        doPost("/api/entityView", savedView, EntityView.class);

        doDelete("/api/asset/" + savedAsset1.getId().getId().toString())
                .andExpect(status().isOk());

        String assetIdStr = savedAsset1.getId().getId().toString();
        doGet("/api/asset/" + assetIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(classNameAsset, assetIdStr))));
    }

    @Test
    public void testSaveAssetWithEmptyType() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");

        Mockito.reset(tbClusterService, auditLogService);

        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Assert.assertEquals("default", savedAsset.getType());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedAsset, savedAsset.getId(), savedAsset.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
    }

    @Test
    public void testSaveAssetWithEmptyName() throws Exception {
        Asset asset = new Asset();
        asset.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Asset name " + msgErrorShouldBeSpecified;
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new ThingsboardException(msgError,
                        ThingsboardErrorCode.PERMISSION_DENIED));
        testNotifyEntityEqualsOneTimeServiceNeverError(asset, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindTenantAssets() throws Exception {
        List<Asset> assets = new ArrayList<>();
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            Asset asset = new Asset();
            asset.setName("Asset" + i);
            asset.setType("default");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Asset(), new Asset(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);

        loadedAssets.removeIf(asset -> asset.getType().equals(DefaultRuleEngineStatisticsService.TB_SERVICE_QUEUE));

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);
    }

    @Test
    public void testFindTenantAssetsByName() throws Exception {
        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<PageData<Asset>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantAssetsByType() throws Exception {
        String title1 = "Asset title 1";
        String type1 = "typeA";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        String type2 = "typeB";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            Asset asset = new Asset();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type1);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<PageData<Asset>>() {
                    }, pageLink, type2);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            doDelete("/api/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<PageData<Asset>>() {
                }, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testDeleteAssetWithDeleteRelationsOk() throws Exception {
        AssetId assetId = createAsset("Asset for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), assetId, "/api/asset/" + assetId);
    }

    @Ignore
    @Test
    public void testDeleteAssetExceptionWithRelationsTransactional() throws Exception {
        AssetId assetId = createAsset("Asset for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(assetDao, savedTenant.getId(), assetId, "/api/asset/" + assetId);
    }

    private Asset createAsset(String name) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType("default");
        return doPost("/api/asset", asset, Asset.class);
    }
}
