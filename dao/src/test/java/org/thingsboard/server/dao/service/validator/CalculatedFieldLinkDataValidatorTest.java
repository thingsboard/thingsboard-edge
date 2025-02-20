/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cf.CalculatedFieldLinkDao;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = CalculatedFieldLinkDataValidator.class)
public class CalculatedFieldLinkDataValidatorTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("2ba09d99-6143-43dc-b645-381fc0c43ebe"));
    private final CalculatedFieldLinkId CALCULATED_FIELD_LINK_ID = new CalculatedFieldLinkId(UUID.fromString("a5609ef4-cb42-43ce-9b23-e090a4878d1c"));

    @MockBean
    private CalculatedFieldLinkDao calculatedFieldLinkDao;
    @SpyBean
    private CalculatedFieldLinkDataValidator validator;

    @Test
    public void testUpdateNonExistingCalculatedField() {
        CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink(CALCULATED_FIELD_LINK_ID);
        calculatedFieldLink.setCalculatedFieldId(new CalculatedFieldId(UUID.fromString("136477af-fd07-4498-b9c9-54fe50e82992")));

        given(calculatedFieldLinkDao.findById(TENANT_ID, CALCULATED_FIELD_LINK_ID.getId())).willReturn(null);

        assertThatThrownBy(() -> validator.validateUpdate(TENANT_ID, calculatedFieldLink))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't update non existing calculated field link!");
    }

}
