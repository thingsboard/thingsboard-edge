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
package org.thingsboard.rule.engine.report;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.nio.ByteBuffer;
import java.util.UUID;

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
                    JsonNode msgJson = JacksonUtil.toJsonNode(msg.getData());
                    JsonNode reportConfigJson = msgJson.get("reportConfig");
                    reportConfig = JacksonUtil.treeToValue(reportConfigJson, ReportConfig.class);
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

}
