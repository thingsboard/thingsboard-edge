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


import com.google.common.util.concurrent.ListenableFuture;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.blob.BaseBlobEntityService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.Collections;
import java.util.List;

@Profile("test")
@Configuration
public class BlobEntityServiceTest {

    @Autowired
    BaseBlobEntityService baseBlobEntityService;

    @Autowired
    protected JpaExecutorService service;

    @Bean
    @Primary
    public BlobEntityService blobEntityService() throws ThingsboardException {
        BlobEntityService blobEntityService = Mockito.mock(BlobEntityService.class);

        Mockito.doAnswer(new Answer<BlobEntityWithCustomerInfo>() {
            public BlobEntityWithCustomerInfo answer(InvocationOnMock invocationOnMock) {
                return new BlobEntityWithCustomerInfo();
            }
        }).when(blobEntityService).findBlobEntityWithCustomerInfoById(Mockito.any(TenantId.class), Mockito.any(BlobEntityId.class));
        Mockito.doAnswer(new Answer<BlobEntity>() {
            public BlobEntity answer(InvocationOnMock invocationOnMock) {
                return new BlobEntity();
            }
        }).when(blobEntityService).findBlobEntityById(Mockito.any(TenantId.class), Mockito.any(BlobEntityId.class));
        Mockito.doAnswer(new Answer<PageData<BlobEntityWithCustomerInfo>>() {
            public PageData<BlobEntityWithCustomerInfo> answer(InvocationOnMock invocationOnMock) {
                return new PageData<>();
            }
        }).when(blobEntityService).findBlobEntitiesByTenantIdAndType(Mockito.any(TenantId.class), Mockito.anyString(), Mockito.any(TimePageLink.class));
        Mockito.doAnswer(new Answer<PageData<BlobEntityWithCustomerInfo>>() {
            public PageData<BlobEntityWithCustomerInfo> answer(InvocationOnMock invocationOnMock) {
                return new PageData<>();
            }
        }).when(blobEntityService).findBlobEntitiesByTenantId(Mockito.any(TenantId.class), Mockito.any(TimePageLink.class));
        Mockito.doAnswer(new Answer<PageData<BlobEntityWithCustomerInfo>>() {
            public PageData<BlobEntityWithCustomerInfo> answer(InvocationOnMock invocationOnMock) {
                return new PageData<>();
            }
        }).when(blobEntityService).findBlobEntitiesByTenantIdAndCustomerIdAndType(
                Mockito.any(TenantId.class), Mockito.any(CustomerId.class), Mockito.anyString(), Mockito.any(TimePageLink.class));
        Mockito.doAnswer(new Answer<PageData<BlobEntityWithCustomerInfo>>() {
            public PageData<BlobEntityWithCustomerInfo> answer(InvocationOnMock invocationOnMock) {
                return new PageData<>();
            }
        }).when(blobEntityService).findBlobEntitiesByTenantIdAndCustomerId(
                Mockito.any(TenantId.class), Mockito.any(CustomerId.class), Mockito.any(TimePageLink.class));
        Mockito.doAnswer(new Answer<ListenableFuture<List<BlobEntityInfo>>>() {
            public ListenableFuture<List<BlobEntityInfo>> answer(InvocationOnMock invocationOnMock) {
                List<BlobEntityInfo> list = Collections.emptyList();
                return service.submit(() -> list);
            }
        }).when(blobEntityService).findBlobEntityInfoByIdsAsync(
                Mockito.any(TenantId.class), Mockito.any(List.class));
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocationOnMock) {
                return null;
            }
        }).when(blobEntityService).deleteBlobEntity(Mockito.any(TenantId.class), Mockito.any(BlobEntityId.class));
        return blobEntityService;
    }
}
