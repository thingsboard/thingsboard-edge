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
package org.thingsboard.server.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Illia Barkov
 */

@Slf4j
public abstract class BaseRestApiLimitsTest extends AbstractControllerTest {

    private static int MESSAGES_LIMIT = 10;
    private static int TIME_FOR_LIMIT = 5;

    TenantProfile tenantProfile;

    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    @Before
    public void before() throws Exception {
        loginSysAdmin();
        tenantProfile = getDefaultTenantProfile();
        resetTokens();
    }

    @After
    public void after() throws Exception {
        resetTokens();
        loginSysAdmin();
        saveTenantProfileWitConfiguration(tenantProfile, new DefaultTenantProfileConfiguration());
        resetTokens();
        service.shutdown();
    }

    @Test
    public void testCustomerRestApiLimits() throws Exception {
        loginSysAdmin();

        String customerRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithCustomerRestLimits = createTenantProfileConfigurationWithRestLimits(null, customerRestLimit);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithCustomerRestLimits);

        resetTokens();

        loginCustomerUser();

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            doGet("/api/device/types").andExpect(status().isOk());
        }
        doGet("/api/device/types").andExpect(status().is4xxClientError());
    }

    @Test
    public void testTenantRestApiLimits() throws Exception {
        loginSysAdmin();

        String tenantRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithTenantRestLimits = createTenantProfileConfigurationWithRestLimits(tenantRestLimit, null);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithTenantRestLimits);

        resetTokens();

        loginCustomerUser();

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            doGet("/api/device/types").andExpect(status().isOk());
        }
        doGet("/api/device/types").andExpect(status().is4xxClientError());
    }

    @Test
    public void testCustomerRestApiLimitsWithAsyncMethod() throws Exception {
        loginSysAdmin();

        String tenantRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithTenantRestLimits = createTenantProfileConfigurationWithRestLimits(tenantRestLimit, null);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithTenantRestLimits);

        resetTokens();

        loginTenantAdmin();

        List<ListenableFuture<ResultActions>> attributesRequests = new ArrayList<>();

        doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes").andExpect(status().isOk());
        Thread.sleep(TimeUnit.SECONDS.toMillis(TIME_FOR_LIMIT)); // Wait to initialization for bucket4j

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            attributesRequests.add(service.submit(() -> doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes")));
        }

        List<ResultActions> lists = blockForResponses(attributesRequests);

        for (ResultActions resultActions : lists) {
            resultActions.andExpect(status().isOk());
        }

        doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes").andExpect(status().is4xxClientError());
    }

    private TenantProfile getDefaultTenantProfile() throws Exception {

        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        List<TenantProfile> tenantProfiles = new ArrayList<>(pageData.getData());

        Optional<TenantProfile> optionalDefaultProfile = tenantProfiles.stream().filter(TenantProfile::isDefault).reduce((a, b) -> null);
        Assert.assertTrue(optionalDefaultProfile.isPresent());

        return optionalDefaultProfile.get();
    }

    List<ResultActions> blockForResponses(List<ListenableFuture<ResultActions>> futures) throws ExecutionException {
        ListenableFuture<List<ResultActions>> futureOfList = Futures.allAsList(futures);
        List<ResultActions> responses;
        try {
            responses = futureOfList.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            responses = new ArrayList<>();
            for (ListenableFuture<ResultActions> future : futures) {
                if (future.isDone()) {
                    responses.add(Uninterruptibles.getUninterruptibly(future));
                }
            }
        }
        return responses;
    }

    private DefaultTenantProfileConfiguration createTenantProfileConfigurationWithRestLimits(String tenantLimits, String customerLimits) {
        DefaultTenantProfileConfiguration.DefaultTenantProfileConfigurationBuilder builder = DefaultTenantProfileConfiguration.builder();
        builder.tenantServerRestLimitsConfiguration(tenantLimits);
        builder.customerServerRestLimitsConfiguration(customerLimits);
        return builder.build();

    }

    private void saveTenantProfileWitConfiguration(TenantProfile tenantProfile, TenantProfileConfiguration tenantProfileConfiguration) {
        TenantProfileData tenantProfileData = tenantProfile.getProfileData();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
    }

}
