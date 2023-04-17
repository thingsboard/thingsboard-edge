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
package org.thingsboard.server.msa.edge;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

@Slf4j
public class OtaPackageClientTest extends AbstractContainerTest {

    @Test
    public void testOtaPackages() throws Exception {
        // create ota package
        DeviceProfileInfo defaultDeviceProfileInfo = cloudRestClient.getDefaultDeviceProfileInfo();
        OtaPackageId otaPackageId = createOtaPackageInfo(new DeviceProfileId(defaultDeviceProfileInfo.getId().getId()), FIRMWARE);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() ->  {
                    PageData<OtaPackageInfo> otaPackages = edgeRestClient.getOtaPackages(new PageLink(100));
                    if (otaPackages.getData().size() < 1) {
                        return false;
                    }
                    OtaPackage otaPackageById = edgeRestClient.getOtaPackageById(otaPackages.getData().get(0).getId());
                    return otaPackageById.isHasData();
                });

        PageData<OtaPackageInfo> pageData = edgeRestClient.getOtaPackages(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.OTA_PACKAGE);

        // delete ota package
        cloudRestClient.deleteOtaPackage(otaPackageId);
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<OtaPackageInfo> otaPackages = edgeRestClient.getOtaPackages(new PageLink(100));
                    if (otaPackages.getData().isEmpty()) {
                        return true;
                    }
                    return otaPackages.getData().stream().map(OtaPackageInfo::getId).noneMatch(otaPackageId::equals);
                });
    }


}

