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
package org.thingsboard.server.controller;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.blob.BaseBlobEntityService;
import org.thingsboard.server.dao.blob.BlobEntityService;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.controller.BlobEntityController.BLOB_ENTITY_ID;

public abstract class BaseBlobEntityControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;
    private final String blobEntityIdStr = "784f394c-42b6-435a-983c-b7beff2784f9";
    private final BlobEntityId blobEntityId = new BlobEntityId(UUID.fromString(blobEntityIdStr));

    @Autowired
    BlobEntityService blobEntityService;

    @Autowired
    BaseBlobEntityService baseBlobEntityService;

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
    public void testDeleteBlobEntity() throws Exception {
        BlobEntityWithCustomerInfo blobEntityWithCustomerInfo = new BlobEntityWithCustomerInfo();
        blobEntityWithCustomerInfo.setId(blobEntityId);
        blobEntityWithCustomerInfo.setTenantId(savedTenant.getId());

        doAnswer((Answer<BlobEntityWithCustomerInfo>) invocationOnMock -> {
            return blobEntityWithCustomerInfo;
        }).when(blobEntityService).findBlobEntityWithCustomerInfoById(Mockito.eq(savedTenant.getId()), Mockito.eq(blobEntityId));
        doAnswer((Answer<Void>) invocationOnMock -> {
            return null;
        }).when(blobEntityService).deleteBlobEntity(Mockito.eq(savedTenant.getId()), Mockito.eq(blobEntityId));

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/blobEntity/" + blobEntityIdStr)
                .andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(blobEntityWithCustomerInfo, blobEntityWithCustomerInfo.getId(), blobEntityWithCustomerInfo.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, blobEntityIdStr);
    }

    @Test
    public void testDeleteNonExistentBlobEntity_ResultNotFound() throws Exception {
        doAnswer((Answer<BlobEntityWithCustomerInfo>) invocationOnMock -> {
            var tenantId = (TenantId)invocationOnMock.getArgument(0);
            var blobEntityId_m = (BlobEntityId)invocationOnMock.getArgument(1);
            return baseBlobEntityService.findBlobEntityWithCustomerInfoById(tenantId, blobEntityId_m);
        }).when(blobEntityService).findBlobEntityWithCustomerInfoById(Mockito.eq(savedTenant.getId()), Mockito.eq(blobEntityId));

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/blobEntity/" + blobEntityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Blob entity", blobEntityIdStr))));

        testNotifyEntityNever(null, new BlobEntity());
    }

    @Test
    public void testDeleteBlobEntityParameterIsEmpty() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);

        String blobEntityIdStrEmpty = " ";
        doDelete("/api/blobEntity/" + blobEntityIdStrEmpty)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter '" + BLOB_ENTITY_ID + "' can't be empty!")));

        testNotifyEntityNever(null, new BlobEntity());
    }

    @Test
    public void testDeleteBlobEntityParameterIsInvalid() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);

        String blobEntityIdStrEmpty = "aa";
        doDelete("/api/blobEntity/" + blobEntityIdStrEmpty)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid UUID string: " + blobEntityIdStrEmpty)));

        testNotifyEntityNever(null, new BlobEntity());
    }
}
