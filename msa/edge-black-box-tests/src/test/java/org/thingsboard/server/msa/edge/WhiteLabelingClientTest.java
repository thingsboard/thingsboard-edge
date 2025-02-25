/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.Palette;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WhiteLabelingClientTest extends AbstractContainerTest {

    private static final String ADMIN_PRIMARY_COLOR = "red";
    private static final String TENANT_PRIMARY_COLOR = "blue";
    private static final String CUSTOMER_PRIMARY_COLOR = "green";
    private static final String SUB_CUSTOMER_PRIMARY_COLOR = "yellow";
    private static final String PALETTE_NUMBER = "50";

    private static final String TENANT_CUSTOM_MENU_TITLE = "Tenant Custom Menu";
    private static final String CUSTOMER_CUSTOM_MENU_TITLE = "Customer Custom Menu";
    private static final String SUB_CUSTOMER_CUSTOM_MENU_TITLE = "Sub Customer Custom Menu";

    private static final String SYSADMIN_APP_TITLE = "Sysadmin App Title";
    private static final String TENANT_APP_TITLE = "Tenant App Title";
    private static final String CUSTOMER_APP_TITLE = "Customer App Title";
    private static final String SUB_CUSTOMER_APP_TITLE = "Sub Customer App Title";

    private static final String TENANT_DOMAIN_NAME = "tenant-domain.org";
    private static final String CUSTOMER_DOMAIN_NAME = "customer-domain.org";
    private static final String SUB_CUSTOMER_DOMAIN_NAME = "sub-customer-domain.org";

    private static final String CUSTOMER_TITLE = "Edge Customer A";
    private static final String SUB_CUSTOMER_TITLE = "Edge Sub Customer A";
    private static final String CUSTOMER_PASSWORD = "customer";
    private static final String CUSTOMER_EMAIL = "edgeCustomer@thingsboard.org";
    private static final String SUB_CUSTOMER_EMAIL = "edgeSubCustomer@thingsboard.org";

    private CustomerId parentCustomerId;

    @Test
    public void testWhiteLabeling_LoginWhiteLabeling_CustomMenu() {
        performTestOnEachEdge(this::_testWhiteLabeling_LoginWhiteLabeling_CustomMenu);
    }

    private void _testWhiteLabeling_LoginWhiteLabeling_CustomMenu() {
        testSysAdmin();

        testTenant();

        testCustomer();

        testSubCustomer();

        cleanUp();
    }

    private void testSysAdmin() {
        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        testWhiteLabelAndLoginParams(SYSADMIN_APP_TITLE, ADMIN_PRIMARY_COLOR, true, null);
    }

    private void testTenant() {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        testWhiteLabelAndLoginParams(TENANT_APP_TITLE, TENANT_PRIMARY_COLOR, false, TENANT_DOMAIN_NAME);
        updateAndVerifyCustomMenuUpdate(TENANT_CUSTOM_MENU_TITLE, CMScope.TENANT);
    }

    private void testCustomer() {
        createCustomerAndLogin(CUSTOMER_TITLE, CUSTOMER_EMAIL, null);

        testWhiteLabelAndLoginParams(CUSTOMER_APP_TITLE, CUSTOMER_PRIMARY_COLOR, false, CUSTOMER_DOMAIN_NAME);
        updateAndVerifyCustomMenuUpdate(CUSTOMER_CUSTOM_MENU_TITLE, CMScope.CUSTOMER);
    }

    private void testSubCustomer() {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        Optional<Customer> parentCustomerOpt = cloudRestClient.getTenantCustomer(CUSTOMER_TITLE);

        createCustomerAndLogin(SUB_CUSTOMER_TITLE, SUB_CUSTOMER_EMAIL, parentCustomerOpt.get().getId());

        testWhiteLabelAndLoginParams(SUB_CUSTOMER_APP_TITLE, SUB_CUSTOMER_PRIMARY_COLOR, false, SUB_CUSTOMER_DOMAIN_NAME);
        updateAndVerifyCustomMenuUpdate(SUB_CUSTOMER_CUSTOM_MENU_TITLE, CMScope.CUSTOMER);
    }

    private void cleanUp() {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        Optional<Customer> parentCustomerOpt = cloudRestClient.getTenantCustomer(CUSTOMER_TITLE);

        changeOwnerToTenantAndRemoveCustomer(parentCustomerOpt.get());

        cloudRestClient.saveWhiteLabelParams(new WhiteLabelingParams());
        Optional<LoginWhiteLabelingParams> currentLoginWhiteLabelParamsOpt  = cloudRestClient.getCurrentLoginWhiteLabelParams();
        LoginWhiteLabelingParams currentLoginWhiteLabelParams = currentLoginWhiteLabelParamsOpt.get();
        currentLoginWhiteLabelParams.setPaletteSettings(null);
        cloudRestClient.saveLoginWhiteLabelParams(currentLoginWhiteLabelParams);

        cloudRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        cloudRestClient.saveWhiteLabelParams(new WhiteLabelingParams());
        cloudRestClient.saveLoginWhiteLabelParams(new LoginWhiteLabelingParams());
    }

    private void testWhiteLabelAndLoginParams(String appTitle, String primaryColor, boolean isSysAdmin, String domainName) {
        Optional<WhiteLabelingParams> currentWhiteLabelParamsOpt = cloudRestClient.getCurrentWhiteLabelParams();
        WhiteLabelingParams whiteLabelingParams = currentWhiteLabelParamsOpt.orElse(new WhiteLabelingParams());
        whiteLabelingParams.setAppTitle(appTitle);
        whiteLabelingParams.setPaletteSettings(createPaletteSettings(primaryColor));
        cloudRestClient.saveWhiteLabelParams(whiteLabelingParams);

        if (!isSysAdmin) {
            Awaitility.await()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(30, TimeUnit.SECONDS)
                    .until(() -> {
                        Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getCurrentWhiteLabelParams();
                        Optional<WhiteLabelingParams> cloudWhiteLabelParams = cloudRestClient.getCurrentWhiteLabelParams();
                        return edgeWhiteLabelParams.isPresent() &&
                                cloudWhiteLabelParams.isPresent() &&
                                edgeWhiteLabelParams.get().equals(cloudWhiteLabelParams.get());
                    });
        }

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<WhiteLabelingParams> edgeWhiteLabelParams = edgeRestClient.getWhiteLabelParams(null, null);
                    String edgePrimaryColor = getPrimaryColor(edgeWhiteLabelParams);
                    String edgeAppTitle = edgeWhiteLabelParams.map(WhiteLabelingParams::getAppTitle).orElse(null);
                    return primaryColor.equals(edgePrimaryColor) && appTitle.equals(edgeAppTitle);
                });


        Domain domain;
        if (!isSysAdmin) {
            domain = getOrCreateDomain(domainName);
            Awaitility.await()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(30, TimeUnit.SECONDS)
                    .until(() -> {
                        Optional<DomainInfo> domainInfo = edgeRestClient.getDomainInfoById(domain.getId());
                        return domainInfo.isPresent();
                    });
        } else {
            domain = null;
        }

        LoginWhiteLabelingParams loginWhiteLabelingParams = new LoginWhiteLabelingParams();
        if (!isSysAdmin) {
            loginWhiteLabelingParams.setDomainId(domain.getId());
        }
        loginWhiteLabelingParams.setPaletteSettings(createPaletteSettings(primaryColor));
        cloudRestClient.saveLoginWhiteLabelParams(loginWhiteLabelingParams);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getLoginWhiteLabelParams(null, null);
                    String edgePrimaryColor = getPrimaryColor(edgeLoginWhiteLabelParams);
                    if (!primaryColor.equals(edgePrimaryColor)) {
                        return false;
                    }
                    if (isSysAdmin) {
                        return true;
                    }
                    DomainId edgeDomainId = getDomainId(edgeLoginWhiteLabelParams);
                    return domain.getId().equals(edgeDomainId);
                });

        if (!isSysAdmin) {
            Awaitility.await()
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .atMost(30, TimeUnit.SECONDS)
                    .until(() -> {
                        Optional<LoginWhiteLabelingParams> edgeLoginWhiteLabelParams = edgeRestClient.getCurrentLoginWhiteLabelParams();
                        Optional<LoginWhiteLabelingParams> cloudLoginWhiteLabelParams = cloudRestClient.getCurrentLoginWhiteLabelParams();
                        return edgeLoginWhiteLabelParams.isPresent() &&
                                cloudLoginWhiteLabelParams.isPresent() &&
                                edgeLoginWhiteLabelParams.get().equals(cloudLoginWhiteLabelParams.get());
                    });
        }
    }

    private Domain getOrCreateDomain(String domainName) {
        PageData<DomainInfo> tenantDomains = cloudRestClient.getTenantDomainInfos(new PageLink(1024));
        for (DomainInfo domain : tenantDomains.getData()) {
            if (domain.getName().equals(domainName)) {
                return domain;
            }
        }

        Domain domain = new Domain();
        domain.setName(domainName);
        domain.setOauth2Enabled(true);
        domain.setPropagateToEdge(true);
        return cloudRestClient.saveDomain(domain);
    }

    private void createCustomerAndLogin(String customerTitle, String email, CustomerId parentCustomerId) {
        // create customer
        Customer savedCustomer = saveCustomer(customerTitle, parentCustomerId);

        // change owner to customer
        cloudRestClient.changeOwnerToCustomer(savedCustomer.getId(), edge.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isPresent());

        // create user
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(edge.getTenantId());
        user.setCustomerId(savedCustomer.getId());
        user.setEmail(email);
        User savedUser = cloudRestClient.saveUser(user, false, findCustomerAdminsGroup(savedCustomer).get().getId());
        cloudRestClient.activateUser(savedUser.getId(), CUSTOMER_PASSWORD, false);

        verifyThatCustomerAdminGroupIsCreatedOnEdge(savedCustomer);

        loginIntoEdgeWithRetries(email, CUSTOMER_PASSWORD);
        cloudRestClient.login(email, CUSTOMER_PASSWORD);
    }

    private String getPrimaryColor(Optional<? extends WhiteLabelingParams> whiteLabelingParams) {
        if (whiteLabelingParams.isEmpty() || whiteLabelingParams.get().getPaletteSettings() == null
                || whiteLabelingParams.get().getPaletteSettings().getPrimaryPalette() == null
                || whiteLabelingParams.get().getPaletteSettings().getPrimaryPalette().getColors() == null) {
            return "";
        }
        return whiteLabelingParams.get().getPaletteSettings().getPrimaryPalette().getColors().get(PALETTE_NUMBER);
    }

    private DomainId getDomainId(Optional<LoginWhiteLabelingParams> loginWhiteLabelingParams) {
        return loginWhiteLabelingParams.map(LoginWhiteLabelingParams::getDomainId).orElse(new DomainId(EntityId.NULL_UUID));
    }

    private PaletteSettings createPaletteSettings(String color) {
        PaletteSettings paletteSettings = new PaletteSettings();
        Palette primaryPalette = new Palette();
        primaryPalette.setType("custom");
        HashMap<String, String> colors = new HashMap<>();
        colors.put(PALETTE_NUMBER, color);
        primaryPalette.setColors(colors);
        paletteSettings.setPrimaryPalette(primaryPalette);
        return paletteSettings;
    }

    private void updateAndVerifyCustomMenuUpdate(String customMenuName, CMScope scope) {
        if (!isCustomMenuAlreadyCreated(customMenuName, scope)) {
            CustomMenu menu = new CustomMenu();
            menu.setName(customMenuName);
            menu.setScope(scope);
            menu.setAssigneeType(CMAssigneeType.ALL);
            cloudRestClient.saveCustomMenu(menu, null, false);
        }
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<CustomMenuInfo> edgeCustomMenuInfos = edgeRestClient.getCustomMenuInfos(new PageLink(1024));
                    PageData<CustomMenuInfo> cloudCustomMenuInfos = cloudRestClient.getCustomMenuInfos(new PageLink(1024));
                    return edgeCustomMenuInfos.getTotalElements() == cloudCustomMenuInfos.getTotalElements() &&
                            edgeCustomMenuInfos.getData().equals(cloudCustomMenuInfos.getData());
                });
    }

    private boolean isCustomMenuAlreadyCreated(String customMenuName, CMScope scope) {
        PageData<CustomMenuInfo> cloudCustomMenuInfos = cloudRestClient.getCustomMenuInfos(new PageLink(1024));
        for (CustomMenuInfo datum : cloudCustomMenuInfos.getData()) {
            if (datum.getName().equals(customMenuName)
                    && datum.getScope().equals(scope)) {
                return true;
            }
        }
        return false;
    }

    private void changeOwnerToTenantAndRemoveCustomer(Customer savedCustomer) {
        cloudRestClient.login("tenant@thingsboard.org", "tenant");
        edgeRestClient.login("tenant@thingsboard.org", "tenant");

        // change owner to tenant
        cloudRestClient.changeOwnerToTenant(edge.getTenantId(), edge.getId());

        // validate that customer was deleted from edge
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> edgeRestClient.getCustomerById(savedCustomer.getId()).isEmpty());

        // validate that edge customer id was updated
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> EntityId.NULL_UUID.equals(edgeRestClient.getEdgeById(edge.getId()).get().getCustomerId().getId()));

        // delete customer
        cloudRestClient.deleteCustomer(savedCustomer.getId());
    }

}
