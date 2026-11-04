// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cloud.rpc.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.cloud.CloudEvent;
import org.thingsboard.server.common.data.cloud.CloudEventType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.report.BaseReportTemplateProcessor;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class ReportTemplateCloudProcessor extends BaseReportTemplateProcessor {

    public ListenableFuture<Void> processReportTemplateMsgFromCloud(TenantId tenantId, ReportTemplateUpdateMsg reportTemplateUpdateMsg) {
        ReportTemplateId reportTemplateId = new ReportTemplateId(new UUID(reportTemplateUpdateMsg.getIdMSB(), reportTemplateUpdateMsg.getIdLSB()));
        try {
            cloudSynchronizationManager.getSync().set(true);

            switch (reportTemplateUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    Boolean created = super.saveOrUpdateReportTemplate(tenantId, reportTemplateId, reportTemplateUpdateMsg);
                    if (created) {
                        pushReportTemplateCreatedEventToRuleEngine(tenantId, reportTemplateId);
                        return Futures.immediateFuture(null);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    deleteReportTemplate(tenantId, reportTemplateId);
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(reportTemplateUpdateMsg.getMsgType());
            }
        } finally {
            cloudSynchronizationManager.getSync().remove();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public UplinkMsg convertCloudEventToUplink(CloudEvent cloudEvent) {
        ReportTemplateId reportTemplateId = new ReportTemplateId(cloudEvent.getEntityId());
        switch (cloudEvent.getAction()) {
            case ADDED, UPDATED -> {
                ReportTemplate reportTemplate = edgeCtx.getReportTemplateService().findReportTemplateById(cloudEvent.getTenantId(), reportTemplateId);
                if (reportTemplate != null) {
                    UpdateMsgType msgType = getUpdateMsgType(cloudEvent.getAction());
                    ReportTemplateUpdateMsg reportTemplateUpdateMsg = EdgeMsgConstructorUtils.constructReportTemplateUpdatedMsg(msgType, reportTemplate);
                    return UplinkMsg.newBuilder()
                            .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                            .addReportTemplateUpdateMsg(reportTemplateUpdateMsg).build();
                } else {
                    log.info("Skipping event as report template was not found [{}]", cloudEvent);
                }
            }
            case DELETED -> {
                ReportTemplateUpdateMsg reportTemplateUpdateMsg = EdgeMsgConstructorUtils.constructReportTemplateDeleteMsg(reportTemplateId);
                return UplinkMsg.newBuilder()
                        .setUplinkMsgId(EdgeUtils.nextPositiveInt())
                        .addReportTemplateUpdateMsg(reportTemplateUpdateMsg).build();
            }
        }
        return null;
    }

    @Override
    public CloudEventType getCloudEventType() {
        return CloudEventType.REPORT_TEMPLATE;
    }

    private void pushReportTemplateCreatedEventToRuleEngine(TenantId tenantId, ReportTemplateId reportTemplateId) {
        ReportTemplate reportTemplate = edgeCtx.getReportTemplateService().findReportTemplateById(tenantId, reportTemplateId);
        pushReportTemplateEventToRuleEngine(tenantId, reportTemplate, TbMsgType.ENTITY_CREATED);
    }

}
