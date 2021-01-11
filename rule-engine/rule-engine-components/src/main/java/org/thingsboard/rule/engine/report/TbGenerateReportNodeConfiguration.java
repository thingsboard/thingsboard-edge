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
package org.thingsboard.rule.engine.report;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.report.ReportConfig;

@Data
public class TbGenerateReportNodeConfiguration implements NodeConfiguration<TbGenerateReportNodeConfiguration> {

    private boolean useSystemReportsServer;
    private String reportsServerEndpointUrl;
    private boolean useReportConfigFromMessage;
    private ReportConfig reportConfig;

    @Override
    public TbGenerateReportNodeConfiguration defaultConfiguration() {
        TbGenerateReportNodeConfiguration configuration = new TbGenerateReportNodeConfiguration();
        configuration.setUseSystemReportsServer(true);
        configuration.setUseReportConfigFromMessage(true);
        return configuration;
    }
}
