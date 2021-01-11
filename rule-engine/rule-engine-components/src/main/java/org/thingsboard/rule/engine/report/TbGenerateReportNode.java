/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "generate report",
        configClazz = TbGenerateReportNodeConfiguration.class,
        nodeDescription = "Generates report",
        nodeDetails = "Generates dashboard based reports.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGenerateReportConfig",
        icon = "description"
)

public class TbGenerateReportNode implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String ATTACHMENTS = "attachments";

    private TbGenerateReportNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGenerateReportNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ReportConfig reportConfig;
        try {
            if (this.config.isUseReportConfigFromMessage()) {
                try {
                    JsonNode msgJson = mapper.readTree(msg.getData());
                    JsonNode reportConfigJson = msgJson.get("reportConfig");
                    reportConfig = mapper.treeToValue(reportConfigJson, ReportConfig.class);
                } catch (Exception e) {
                    throw new RuntimeException("Incoming message doesn't contain valid reportConfig JSON configuration!", e);
                }
            } else {
                reportConfig = this.config.getReportConfig();
            }
            String reportsServerEndpointUrl = null;
            if (!this.config.isUseSystemReportsServer()) {
                reportsServerEndpointUrl = this.config.getReportsServerEndpointUrl();
            }
            ctx.getPeContext().getReportService().generateReport(
                    ctx.getTenantId(),
                    reportConfig,
                    reportsServerEndpointUrl,
                    reportData -> {
                        User user = ctx.getUserService().findUserById(ctx.getTenantId(), new UserId(UUID.fromString(reportConfig.getUserId())));
                        BlobEntity reportBlobEntity = new BlobEntity();
                        reportBlobEntity.setData(ByteBuffer.wrap(reportData.getData()));
                        reportBlobEntity.setContentType(reportData.getContentType());
                        reportBlobEntity.setName(reportData.getName());
                        reportBlobEntity.setType("report");
                        reportBlobEntity.setTenantId(user.getTenantId());
                        reportBlobEntity.setCustomerId(user.getCustomerId());
                        reportBlobEntity = ctx.getPeContext().getBlobEntityService().saveBlobEntity(reportBlobEntity);
                        TbMsgMetaData metaData = msg.getMetaData().copy();
                        String attachments = metaData.getValue(ATTACHMENTS);
                        if (!StringUtils.isEmpty(attachments)) {
                            attachments += "," + reportBlobEntity.getId().toString();
                        } else {
                            attachments = reportBlobEntity.getId().toString();
                        }
                        metaData.putValue(ATTACHMENTS, attachments);
                        TbMsg newMsg = ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, msg.getData());
                        ctx.tellNext(newMsg, SUCCESS);
                    },
                    throwable -> {
                        ctx.tellFailure(msg, throwable);
                    }
            );
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void destroy() {
    }
}
