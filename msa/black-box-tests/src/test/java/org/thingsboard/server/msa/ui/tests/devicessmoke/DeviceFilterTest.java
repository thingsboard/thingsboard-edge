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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Epic("Filter devices (By device profile and state)")
public class DeviceFilterTest extends AbstractDeviceTest {

    private String activeDeviceName;
    private String deviceWithProfileName;
    private String activeDeviceWithProfileName;

    @BeforeClass
    public void createTestEntities() {
        DeviceProfile deviceProfile = testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(ENTITY_NAME + random()));
        Device deviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));
        Device activeDevice = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random()));
        Device activeDeviceWithProfile = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random(), deviceProfile.getId()));

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(activeDevice.getId());
        DeviceCredentials deviceCredentials1 = testRestClient.getDeviceCredentialsByDeviceId(activeDeviceWithProfile.getId());
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));
        testRestClient.postTelemetry(deviceCredentials1.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));

        deviceProfileTitle = deviceProfile.getName();
        deviceWithProfileName = deviceWithProfile.getName();
        activeDeviceName = activeDevice.getName();
        activeDeviceWithProfileName = activeDeviceWithProfile.getName();
    }

    @AfterClass
    public void deleteTestEntities() {
        deleteDevicesByName(List.of(deviceWithProfileName, activeDeviceName, activeDeviceWithProfileName));
        deleteDeviceProfileByTitle(deviceProfileTitle);
    }

    @Test(groups = "smoke")
    @Description("Filter by device profile")
    public void filterDevicesByProfile() {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfile(deviceProfileTitle);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile(%s) on the page", deviceProfileTitle)
                .isEqualTo(deviceProfileTitle));
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "filterData")
    @Description("Filter by state")
    public void filterDevicesByState(String state) {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByState(state);

        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with '%s' state on the page", state)
                .isEqualTo(state));
    }

    @Test(groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "filterData")
    @Description("Filter device by device profile and state")
    public void filterDevicesByDeviceProfileAndState(String state) {
        sideBarMenuView.goToDevicesPage();
        devicePage.filterBtn().click();
        devicePage.filterDeviceByDeviceProfileAndState(deviceProfileTitle, state);

        devicePage.listOfDevicesProfile().forEach(d -> assertThat(d.getText())
                .as("There are only devices with the selected profile(%s) on the page", deviceProfileTitle)
                .isEqualTo(deviceProfileTitle));
        devicePage.listOfDevicesState().forEach(d -> assertThat(d.getText())
                .as("There are only devices with '%s' state on the page", state)
                .isEqualTo(state));
    }
}
