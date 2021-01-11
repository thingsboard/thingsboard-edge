/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportData;

import java.util.function.Consumer;

public interface ReportService {

    void generateDashboardReport(String baseUrl,
                                 DashboardId dashboardId,
                                 TenantId tenantId,
                                 UserId userId,
                                 String publicId,
                                 String reportName,
                                 JsonNode reportParams,
                                 Consumer<ReportData> onSuccess,
                                 Consumer<Throwable> onFailure);

    void generateReport(TenantId tenantId, ReportConfig reportConfig,
                        String reportsServerEndpointUrl,
                        Consumer<ReportData> onSuccess,
                        Consumer<Throwable> onFailure);

}
