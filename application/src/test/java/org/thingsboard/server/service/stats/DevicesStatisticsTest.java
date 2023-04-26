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
package org.thingsboard.server.service.stats;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.state.DeviceStateService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=2",
        "usage.stats.gauge_report_interval=1",
        "state.defaultStateCheckIntervalInSec=3",
        "state.defaultInactivityTimeoutInSec=10"
})
public class DevicesStatisticsTest extends AbstractControllerTest {

    @Autowired
    private TbApiUsageStateService apiUsageStateService;
    @Autowired
    private TimeseriesService timeseriesService;
    @Autowired
    private DeviceStateService deviceStateService;

    private ApiUsageStateId apiUsageStateId;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        apiUsageStateId = apiUsageStateService.getApiUsageState(tenantId).getId();
    }

    @Test
    public void testDevicesActivityStats() throws Exception {
        int activeDevicesCount = 5;
        List<Device> activeDevices = new ArrayList<>();
        for (int i = 1; i <= activeDevicesCount; i++) {
            String name = "active_device_" + i;
            Device device = createDevice(name, name);
            activeDevices.add(device);
        }
        int inactiveDevicesCount = 10;
        List<Device> inactiveDevices = new ArrayList<>();
        for (int i = 1; i <= inactiveDevicesCount; i++) {
            String name = "inactive_device_" + i;
            Device device = createDevice(name, name);
            inactiveDevices.add(device);
        }

        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, false)).isZero();
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, false)).isEqualTo(activeDevicesCount + inactiveDevicesCount);
                });

        for (Device device : activeDevices) {
            deviceStateService.onDeviceActivity(tenantId, device.getId(), System.currentTimeMillis());
        }

        await().atMost(40, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(getLatestStats(ApiUsageRecordKey.ACTIVE_DEVICES, false)).isEqualTo(activeDevicesCount);
                    assertThat(getLatestStats(ApiUsageRecordKey.INACTIVE_DEVICES, false)).isEqualTo(inactiveDevicesCount);
                });
    }

    @SneakyThrows
    private Long getLatestStats(ApiUsageRecordKey key, boolean hourly) {
        return timeseriesService.findLatest(tenantId, apiUsageStateId, List.of(key.getApiCountKey() + (hourly ? "Hourly" : "")))
                .get().stream().findFirst().flatMap(KvEntry::getLongValue).orElse(null);
    }

}
