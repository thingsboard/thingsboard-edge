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
package org.thingsboard.server.service.sql;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class SequentialTimeseriesPersistenceTest extends AbstractControllerTest {

    final String TOTALIZER = "Totalizer";
    final int TTL = 99999;
    final String GENERIC_CUMULATIVE_OBJ = "genericCumulativeObj";
    final List<Long> ts = List.of(10L, 20L, 30L, 40L, 60L, 70L, 50L, 80L);
    final List<Long> msgValue = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);

    @Autowired
    TimeseriesService timeseriesService;

    TbMsgTimeseriesNodeConfiguration configuration;
    Tenant savedTenant;
    User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        configuration = new TbMsgTimeseriesNodeConfiguration();
        configuration.setUseServerTs(true);

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
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
    }

    @Test
    public void testSequentialTimeseriesPersistence() throws Exception {
        Asset asset = saveAsset("Asset");

        Device deviceA = saveDevice("Device A");
        Device deviceB = saveDevice("Device B");
        Device deviceC = saveDevice("Device C");
        Device deviceD = saveDevice("Device D");
        List<Device> devices = List.of(deviceA, deviceB, deviceC, deviceD);

        for (int i = 0; i < 2; i++) {
            int idx = i * devices.size();
            saveLatestTsForAssetAndDevice(devices, asset, idx);
            checkDiffBetweenLatestTsForDevicesAndAsset(devices, asset);
        }
    }

    Device saveDevice(String name) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(savedDevice);
        return savedDevice;
    }

    Asset saveAsset(String name) throws Exception {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Assert.assertNotNull(savedAsset);
        return savedAsset;
    }

    void saveLatestTsForAssetAndDevice(List<Device> devices, Asset asset, int idx) throws ExecutionException, InterruptedException, TimeoutException {
        for (Device device : devices) {
            TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                    device.getId(),
                    getTbMsgMetadata(device.getName(), ts.get(idx)),
                    TbMsgDataType.JSON,
                    getTbMsgData(msgValue.get(idx)));
            saveDeviceTsEntry(device.getId(), tbMsg, msgValue.get(idx));
            saveAssetTsEntry(asset, device.getName(), msgValue.get(idx), TbMsgTimeseriesNode.computeTs(tbMsg, configuration.isUseServerTs()));
            idx++;
        }
    }

    void checkDiffBetweenLatestTsForDevicesAndAsset(List<Device> devices, Asset asset) throws ExecutionException, InterruptedException, TimeoutException {
        TsKvEntry assetTsKvEntry = getTsKvLatest(asset.getId(), GENERIC_CUMULATIVE_OBJ);
        Assert.assertTrue(assetTsKvEntry.getJsonValue().isPresent());
        JsonObject assetJsonObject = new JsonParser().parse(assetTsKvEntry.getJsonValue().get()).getAsJsonObject();
        for (Device device : devices) {
            Long assetValue = assetJsonObject.get(device.getName()).getAsLong();
            TsKvEntry deviceLatest = getTsKvLatest(device.getId(), TOTALIZER);
            Assert.assertTrue(deviceLatest.getLongValue().isPresent());
            Long deviceValue = deviceLatest.getLongValue().get();
            Assert.assertEquals(assetValue, deviceValue);
        }
    }

    String getTbMsgData(long value) {
        return "{\"Totalizer\": " + value + "}";
    }

    TbMsgMetaData getTbMsgMetadata(String name, long ts) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("deviceName", name);
        metadata.put("ts", String.valueOf(ts));
        return new TbMsgMetaData(metadata);
    }

    void saveDeviceTsEntry(EntityId entityId, TbMsg tbMsg, long value) throws ExecutionException, InterruptedException, TimeoutException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(TbMsgTimeseriesNode.computeTs(tbMsg, configuration.isUseServerTs()), new LongDataEntry(TOTALIZER, value));
        saveTimeseries(entityId, tsKvEntry);
    }

    void saveAssetTsEntry(Asset asset, String key, long value, long ts) throws ExecutionException, InterruptedException, TimeoutException {
        Optional<String> tsKvEntryOpt = getTsKvLatest(asset.getId(), GENERIC_CUMULATIVE_OBJ).getJsonValue();
        TsKvEntry saveTsKvEntry = new BasicTsKvEntry(ts, new JsonDataEntry(GENERIC_CUMULATIVE_OBJ, getJsonObject(key, value, tsKvEntryOpt).toString()));
        saveTimeseries(asset.getId(), saveTsKvEntry);
    }

    JsonObject getJsonObject(String key, long value, Optional<String> tsKvEntryOpt) {
        JsonObject jsonObject = new JsonObject();
        if (tsKvEntryOpt.isPresent()) {
            jsonObject = new JsonParser().parse(tsKvEntryOpt.get()).getAsJsonObject();
        }
        jsonObject.addProperty(key, value);
        return jsonObject;
    }

    void saveTimeseries(EntityId entityId, TsKvEntry saveTsKvEntry) throws InterruptedException, ExecutionException, TimeoutException {
        timeseriesService.save(savedTenant.getId(), entityId, List.of(saveTsKvEntry), TTL).get(TIMEOUT, TimeUnit.SECONDS);
    }

    TsKvEntry getTsKvLatest(EntityId entityId, String key) throws InterruptedException, ExecutionException, TimeoutException {
        List<TsKvEntry> tsKvEntries = timeseriesService.findLatest(
                savedTenant.getTenantId(),
                entityId,
                List.of(key)).get(TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(1, tsKvEntries.size());
        return tsKvEntries.get(0);
    }
}
