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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

@Epic("Make device group private")
public class MakeDeviceGroupPrivateTest extends AbstractDeviceTest {

    private String deviceGroupName;

    @AfterClass
    public void deletePublicCustomer() {
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
    }

    @BeforeMethod
    public void createPublicDeviceGroup() {
        EntityGroup entityGroup = testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(ENTITY_NAME + random(), EntityType.DEVICE));
        testRestClient.setEntityGroupPublic(entityGroup.getId());
        deviceGroupName = entityGroup.getName();
    }

    @AfterMethod
    public void delete() {
        deleteEntityGroupByName(EntityType.DEVICE, deviceGroupName);
    }

    @Test(groups = "smoke")
    @Description("Make device group private by right side btn")
    public void makeDeviceGroupPrivateByRightSideBtn() {
        sideBarMenuView.goToDeviceGroups();
        devicePage.makeDeviceGroupPrivateByRightSideBtn(deviceGroupName);

        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceGroupName));
    }

    @Test(groups = "smoke")
    @Description("Make device group public by btn on details tab")
    public void makeDeviceGroupPrivateFromDetailsTab() {
        sideBarMenuView.goToDeviceGroups();
        devicePage.detailsBtn(deviceGroupName).click();
        devicePage.makeDeviceGroupPrivateFromDetailsTab();
        devicePage.closeDetailsViewBtn().click();

        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceGroupName));
    }
}
