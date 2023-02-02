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
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {BaseAssetProfileControllerTest.Config.class})
public abstract class BaseAssetProfileControllerTest extends AbstractControllerTest {

    private IdComparator<AssetProfile> idComparator = new IdComparator<>();
    private IdComparator<AssetProfileInfo> assetProfileInfoIdComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    private AssetProfileDao assetProfileDao;

    @SpyBean
    private UserPermissionsService userPermissionsService;

    static class Config {
        @Bean
        @Primary
        public AssetProfileDao assetProfileDao(AssetProfileDao assetProfileDao) {
            return Mockito.mock(AssetProfileDao.class, AdditionalAnswers.delegatesTo(assetProfileDao));
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
    public void testSaveAssetProfile() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertNotNull(savedAssetProfile);
        Assert.assertNotNull(savedAssetProfile.getId());
        Assert.assertTrue(savedAssetProfile.getCreatedTime() > 0);
        Assert.assertEquals(assetProfile.getName(), savedAssetProfile.getName());
        Assert.assertEquals(assetProfile.getDescription(), savedAssetProfile.getDescription());
        Assert.assertEquals(assetProfile.isDefault(), savedAssetProfile.isDefault());
        Assert.assertEquals(assetProfile.getDefaultRuleChainId(), savedAssetProfile.getDefaultRuleChainId());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedAssetProfile, savedAssetProfile.getId(), savedAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        savedAssetProfile.setName("New asset profile");
        doPost("/api/assetProfile", savedAssetProfile, AssetProfile.class);
        AssetProfile foundAssetProfile = doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString(), AssetProfile.class);
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfile.getName());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(foundAssetProfile, foundAssetProfile.getId(), foundAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void saveAssetProfileWithViolationOfValidation() throws Exception {
        String msgError = msgErrorFieldLength("name");

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile createAssetProfile = this.createAssetProfile(StringUtils.randomAlphabetic(300));
        doPost("/api/assetProfile", createAssetProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(createAssetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindAssetProfileById() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        AssetProfile foundAssetProfile = doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString(), AssetProfile.class);
        Assert.assertNotNull(foundAssetProfile);
        Assert.assertEquals(savedAssetProfile, foundAssetProfile);
    }

    @Test
    public void whenGetAssetProfileById_thenPermissionsAreChecked() throws Exception {
        loginTenantAdmin();
        AssetProfile assetProfile = createAssetProfile("Asset profile 1");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        loginDifferentTenant();

        doGet("/api/assetProfile/" + assetProfile.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + "ASSET_PROFILE" + " '" + assetProfile.getName() + "'!")));

        loginTenantAdmin();
        User otherTenantUser = new User();
        otherTenantUser.setEmail("tenant-user@thingsboard.org");
        otherTenantUser.setAuthority(Authority.TENANT_ADMIN);
        otherTenantUser.setTenantId(tenantId);
        otherTenantUser = createUser(otherTenantUser, "12345678");
        Map<Resource, Set<Operation>> permissions = Map.of(Resource.ASSET_PROFILE, Set.of(Operation.READ));
        mockUserPermissions(otherTenantUser.getId(), permissions);

        login(otherTenantUser.getEmail(), "12345678");
        doGet("/api/assetProfile/" + assetProfile.getId())
                .andExpect(status().isOk());

        permissions = Map.of(Resource.ASSET, Set.of(Operation.READ));
        mockUserPermissions(otherTenantUser.getId(), permissions);
        doGet("/api/assetProfile/" + assetProfile.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + "ASSET_PROFILE" + " '" + assetProfile.getName() + "'!")));
    }

    @Test
    public void testFindAssetProfileInfoById() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        AssetProfileInfo foundAssetProfileInfo = doGet("/api/assetProfileInfo/" + savedAssetProfile.getId().getId().toString(), AssetProfileInfo.class);
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    @Test
    public void testFindAssetProfileInfoById_NewCustomerNewUser() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        loginNewCustomerNewUser();

        AssetProfileInfo foundAssetProfileInfo = doGet("/api/assetProfileInfo/" + savedAssetProfile.getId().getId().toString(), AssetProfileInfo.class);
        Assert.assertNotNull(foundAssetProfileInfo);
        Assert.assertEquals(savedAssetProfile.getId(), foundAssetProfileInfo.getId());
        Assert.assertEquals(savedAssetProfile.getName(), foundAssetProfileInfo.getName());
    }

    @Test
    public void whenGetAssetProfileInfoById_thenPermissionsAreChecked() throws Exception {
        AssetProfile assetProfile = createAssetProfile("Asset profile 1");
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        loginDifferentTenant();
        doGet("/api/assetProfileInfo/" + assetProfile.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + "ASSET_PROFILE" + " '" + assetProfile.getName() + "'!")));
    }

    @Test
    public void testFindDefaultAssetProfileInfo() throws Exception {
        AssetProfileInfo foundDefaultAssetProfileInfo = doGet("/api/assetProfileInfo/default", AssetProfileInfo.class);
        Assert.assertNotNull(foundDefaultAssetProfileInfo);
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getId());
        Assert.assertNotNull(foundDefaultAssetProfileInfo.getName());
        Assert.assertEquals("default", foundDefaultAssetProfileInfo.getName());
    }

    @Test
    public void testSetDefaultAssetProfile() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile 1");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        AssetProfile defaultAssetProfile = doPost("/api/assetProfile/" + savedAssetProfile.getId().getId().toString() + "/default", AssetProfile.class);
        Assert.assertNotNull(defaultAssetProfile);
        AssetProfileInfo foundDefaultAssetProfile = doGet("/api/assetProfileInfo/default", AssetProfileInfo.class);
        Assert.assertNotNull(foundDefaultAssetProfile);
        Assert.assertEquals(savedAssetProfile.getName(), foundDefaultAssetProfile.getName());
        Assert.assertEquals(savedAssetProfile.getId(), foundDefaultAssetProfile.getId());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(defaultAssetProfile, defaultAssetProfile.getId(), defaultAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);
    }

    @Test
    public void testSaveAssetProfileWithEmptyName() throws Exception {
        AssetProfile assetProfile = new AssetProfile();

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Asset profile name " + msgErrorShouldBeSpecified;
        doPost("/api/assetProfile", assetProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(assetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveAssetProfileWithSameName() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        doPost("/api/assetProfile", assetProfile).andExpect(status().isOk());
        AssetProfile assetProfile2 = this.createAssetProfile("Asset Profile");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Asset profile with such name already exists";
        doPost("/api/assetProfile", assetProfile2)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(assetProfile, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testDeleteAssetProfileWithExistingAsset() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Asset asset = new Asset();
        asset.setName("Test asset");
        asset.setAssetProfileId(savedAssetProfile.getId());

        doPost("/api/asset", asset, Asset.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("The asset profile referenced by the assets cannot be deleted")));

        testNotifyEntityNever(savedAssetProfile.getId(), savedAssetProfile);
    }

    @Test
    public void testSaveAssetProfileWithRuleChainFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Different rule chain");
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);

        loginTenantAdmin();

        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        assetProfile.setDefaultRuleChainId(savedRuleChain.getId());
        doPost("/api/assetProfile", assetProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign rule chain from different tenant!")));
    }

    @Test
    public void testSaveAssetProfileWithDashboardFromDifferentTenant() throws Exception {
        loginDifferentTenant();
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Different dashboard");
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);

        loginTenantAdmin();

        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        assetProfile.setDefaultDashboardId(savedDashboard.getId());
        doPost("/api/assetProfile", assetProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Can't assign dashboard from different tenant!")));
    }

    @Test
    public void testDeleteAssetProfile() throws Exception {
        AssetProfile assetProfile = this.createAssetProfile("Asset Profile");
        AssetProfile savedAssetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isOk());

        String savedAssetProfileIdStr = savedAssetProfile.getId().getId().toString();
        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedAssetProfile, savedAssetProfile.getId(), savedAssetProfile.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedAssetProfileIdStr);

        doGet("/api/assetProfile/" + savedAssetProfile.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Asset profile", savedAssetProfileIdStr))));
    }

    @Test
    public void testFindAssetProfiles() throws Exception {
        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        assetProfiles.addAll(pageData.getData());

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 28;
        for (int i = 0; i < cntEntity; i++) {
            AssetProfile assetProfile = this.createAssetProfile("Asset Profile" + i);
            assetProfiles.add(doPost("/api/assetProfile", assetProfile, AssetProfile.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new AssetProfile(), new AssetProfile(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ADDED, cntEntity, cntEntity, cntEntity);
        Mockito.reset(tbClusterService, auditLogService);

        List<AssetProfile> loadedAssetProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedAssetProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfiles, idComparator);

        Assert.assertEquals(assetProfiles, loadedAssetProfiles);

        for (AssetProfile assetProfile : loadedAssetProfiles) {
            if (!assetProfile.isDefault()) {
                doDelete("/api/assetProfile/" + assetProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(loadedAssetProfiles.get(0), loadedAssetProfiles.get(0),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, ActionType.DELETED, cntEntity, cntEntity, cntEntity, loadedAssetProfiles.get(0).getId().getId().toString());

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void whenFindAssetProfiles_thenPermissionsAreChecked() throws Exception {
        loginTenantAdmin();
        AssetProfile assetProfile = createAssetProfile("Asset profile 1");

        User otherTenantUser = new User();
        otherTenantUser.setEmail("tenant-user@thingsboard.org");
        otherTenantUser.setAuthority(Authority.TENANT_ADMIN);
        otherTenantUser.setTenantId(tenantId);
        otherTenantUser = createUser(otherTenantUser, "12345678");
        Map<Resource, Set<Operation>> permissions = Map.of(Resource.ALL, Set.of(Operation.READ));
        mockUserPermissions(otherTenantUser.getId(), permissions);

        login(otherTenantUser.getEmail(), "12345678");
        doGet("/api/assetProfiles?pageSize=10&page=0")
                .andExpect(status().isOk());

        permissions = Map.of(Resource.ASSET, Set.of(Operation.READ));
        mockUserPermissions(otherTenantUser.getId(), permissions);
        doGet("/api/assetProfiles?pageSize=10&page=0")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionRead + "'ASSET_PROFILE' resource!")));
    }

    @Test
    public void testFindAssetProfileInfos() throws Exception {
        List<AssetProfile> assetProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<AssetProfile> assetProfilePageData = doGetTypedWithPageLink("/api/assetProfiles?",
                new TypeReference<PageData<AssetProfile>>() {
                }, pageLink);
        Assert.assertFalse(assetProfilePageData.hasNext());
        Assert.assertEquals(1, assetProfilePageData.getTotalElements());
        assetProfiles.addAll(assetProfilePageData.getData());

        for (int i = 0; i < 28; i++) {
            AssetProfile assetProfile = this.createAssetProfile("Asset Profile" + i);
            assetProfiles.add(doPost("/api/assetProfile", assetProfile, AssetProfile.class));
        }

        List<AssetProfileInfo> loadedAssetProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<AssetProfileInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/assetProfileInfos?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedAssetProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetProfiles, idComparator);
        Collections.sort(loadedAssetProfileInfos, assetProfileInfoIdComparator);

        List<AssetProfileInfo> assetProfileInfos = assetProfiles.stream().map(assetProfile -> new AssetProfileInfo(assetProfile.getId(),
                assetProfile.getName(), assetProfile.getImage(), assetProfile.getDefaultDashboardId())).collect(Collectors.toList());

        Assert.assertEquals(assetProfileInfos, loadedAssetProfileInfos);

        for (AssetProfile assetProfile : assetProfiles) {
            if (!assetProfile.isDefault()) {
                doDelete("/api/assetProfile/" + assetProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/assetProfileInfos?",
                new TypeReference<PageData<AssetProfileInfo>>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    protected void mockUserPermissions(UserId userId, Map<Resource, Set<Operation>> permissions) throws ThingsboardException {
        MergedUserPermissions mergedUserPermissions = new MergedUserPermissions(permissions, Collections.emptyMap());
        doReturn(mergedUserPermissions).when(userPermissionsService)
                .getMergedPermissions(argThat(user -> user.getId().equals(userId)), anyBoolean());
    }

    private void loginNewCustomerNewUser()  throws Exception {

        Customer customer = new Customer();
        customer.setTitle("Customer");
        customer.setTenantId(savedTenant.getId());
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Role role = new Role();
        role.setTenantId(savedTenant.getId());
        role.setCustomerId(savedCustomer.getId());
        role.setType(RoleType.GENERIC);
        role.setName("Test customer administrator");
        role.setPermissions(JacksonUtil.toJsonNode("{\"ALL\":[\"ALL\"]}"));

        role = doPost("/api/role", role, Role.class);

        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName("Test customer administrators");
        entityGroup.setType(EntityType.USER);
        entityGroup.setOwnerId(savedCustomer.getId());
        entityGroup = doPost("/api/entityGroup", entityGroup, EntityGroup.class);

        GroupPermission groupPermission = new GroupPermission(
                tenantId,
                entityGroup.getId(),
                role.getId(),
                null,
                null,
                false
        );

        doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(savedTenant.getId());
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customer2@thingsboard.org");

        createUser(customerUser, "customer", entityGroup.getId());

        login("customer2@thingsboard.org", "customer");
    }

    @Test
    public void testDeleteAssetProfileWithDeleteRelationsOk() throws Exception {
        AssetProfileId assetProfileId = savedAssetProfile("AssetProfile for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), assetProfileId, "/api/assetProfile/" + assetProfileId);
    }

    @Test
    public void testDeleteAssetProfileExceptionWithRelationsTransactional() throws Exception {
        AssetProfileId assetProfileId = savedAssetProfile("AssetProfile for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(assetProfileDao, savedTenant.getId(), assetProfileId, "/api/assetProfile/" + assetProfileId);
    }

    private AssetProfile savedAssetProfile(String name) {
        AssetProfile assetProfile = createAssetProfile(name);
        return doPost("/api/assetProfile", assetProfile, AssetProfile.class);
    }
}
