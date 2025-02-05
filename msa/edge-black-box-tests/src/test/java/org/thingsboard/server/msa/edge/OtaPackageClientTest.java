/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.AbstractContainerTest;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

@Slf4j
public class OtaPackageClientTest extends AbstractContainerTest {

    @Test
    public void testOtaPackages() {
        performTestOnEachEdge(this::_testOtaPackages);
    }

    private void _testOtaPackages() {
        // create ota package
        DeviceProfileInfo defaultDeviceProfileInfo = cloudRestClient.getDefaultDeviceProfileInfo();
        DeviceProfileId deviceProfileId = new DeviceProfileId(defaultDeviceProfileInfo.getId().getId());
        OtaPackageId otaPackageId = createOtaPackageInfo(deviceProfileId, FIRMWARE);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() ->  {
                    PageData<OtaPackageInfo> otaPackages = edgeRestClient.getOtaPackages(new PageLink(100));
                    if (otaPackages.getData().isEmpty()) {
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

        testOtaPackage_urlType(deviceProfileId, SOFTWARE);
    }

    private void testOtaPackage_urlType(DeviceProfileId deviceProfileId, OtaPackageType otaPackageType) {
        OtaPackageInfo otaPackageInfo = new OtaPackageInfo();
        otaPackageInfo.setDeviceProfileId(deviceProfileId);
        otaPackageInfo.setType(otaPackageType);
        otaPackageInfo.setTitle("My " + otaPackageType + " #2");
        otaPackageInfo.setVersion("v2.0");
        otaPackageInfo.setTag("My " + otaPackageType + " #2 v2.0");
        otaPackageInfo.setHasData(false);
        otaPackageInfo.setUrl("http://my.url");
        OtaPackageInfo savedOtaPackageInfo = cloudRestClient.saveOtaPackageInfo(otaPackageInfo, true);

        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<OtaPackageInfo> otaPackages = edgeRestClient.getOtaPackages(new PageLink(100));
                    if (otaPackages.getData().isEmpty()) {
                        return false;
                    }
                    return otaPackages.getData().stream().map(OtaPackageInfo::getId).anyMatch(savedOtaPackageInfo.getId()::equals);
                });

        PageData<OtaPackageInfo> pageData = edgeRestClient.getOtaPackages(new PageLink(100));
        assertEntitiesByIdsAndType(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()), EntityType.OTA_PACKAGE);

        // delete ota package
        cloudRestClient.deleteOtaPackage(savedOtaPackageInfo.getId());
        Awaitility.await()
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<OtaPackageInfo> otaPackages = edgeRestClient.getOtaPackages(new PageLink(100));
                    if (otaPackages.getData().isEmpty()) {
                        return true;
                    }
                    return otaPackages.getData().stream().map(OtaPackageInfo::getId).noneMatch(savedOtaPackageInfo.getId()::equals);
                });
    }
}
