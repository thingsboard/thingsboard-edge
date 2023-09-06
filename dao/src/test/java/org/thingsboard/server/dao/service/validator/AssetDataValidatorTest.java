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
package org.thingsboard.server.dao.service.validator;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = AssetDataValidator.class)
@Slf4j
class AssetDataValidatorTest {

    @MockBean
    AssetDao assetDao;
    @MockBean
    TenantService tenantService;
    @MockBean
    CustomerDao customerDao;
    @Autowired
    AssetDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "coffee", "1", "big box", "世界", "!", "--", "~!@#$%^&*()_+=-/|\\[]{};:'`\"?<>,.", "\uD83D\uDC0C", "\041",
            "Gdy Pomorze nie pomoże, to pomoże może morze, a gdy morze nie pomoże, to pomoże może Gdańsk",
    })
    void testAssetName_thenOK(final String name) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(name);
        validator.validateDataImpl(tenantId, asset);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "F0929906\000\000\000\000\000\000\000\000\000", "\000\000\000F0929906",
            "\u0000F0929906", "F092\u00009906", "F0929906\u0000"
    })
    void testAssetName_thenDataValidationException(final String name) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(name);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class, () -> validator.validateDataImpl(tenantId, asset));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Asset name").containsPattern("Asset name .*");
    }

}
