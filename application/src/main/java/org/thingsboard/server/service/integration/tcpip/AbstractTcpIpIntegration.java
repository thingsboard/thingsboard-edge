/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.integration.tcpip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
public abstract class AbstractTcpIpIntegration extends AbstractIntegration<TcpipIntegrationMsg> {

    protected IntegrationContext integrationContext;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        integrationContext = params.getContext();
    }

    @Override
    public void process(IntegrationContext context, TcpipIntegrationMsg msg) {
        String status = "OK";
        Exception exception = null;
        try {
            List<UplinkData> upLinkDataList = getUplinkDataList(context, msg);
            processUplinkData(context, upLinkDataList);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(mapper.readTree(msg.getMsg())), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    public byte[] writeValueAsBytes(String msg) {
        try {
            return mapper.writeValueAsBytes(msg);
        } catch (JsonProcessingException e) {
            log.error("{}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean isEmptyObjectNode(ObjectNode objectNode) {
        if (objectNode == null) {
            return true;
        }
        JsonNode jsonNode = objectNode.get("report");
        return jsonNode == null || (jsonNode.isArray() && (jsonNode.size() == 0 || jsonNode.get(0).size() == 0));
    }

    public boolean isEmptyByteArray(byte[] byteArray) {
        return byteArray == null || byteArray.length == 0;
    }

    protected byte [] toByteArray(ByteBuf buffer) {
        byte [] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    protected ObjectNode getJsonHexReport(byte[] hexBytes) {
        String hexString = Hex.encodeHexString(hexBytes);
        ArrayNode reports = mapper.createArrayNode();
        reports.add(mapper.createObjectNode().put("value", hexString));
        ObjectNode payload = mapper.createObjectNode();
        payload.set("reports", reports);
        return payload;
    }

    private List<UplinkData> getUplinkDataList(IntegrationContext context, TcpipIntegrationMsg msg) throws Exception {
        Map<String, String> metadataMap = new HashMap<>(metadataTemplate.getKvMap());
        return convertToUplinkDataList(context, msg.getMsg(), new UplinkMetaData(getUplinkContentType(), metadataMap));
    }

    private void processUplinkData(IntegrationContext context, List<UplinkData> uplinkDataList) throws Exception {
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            for (UplinkData uplinkData : uplinkDataList) {
                processUplinkData(context, uplinkData);
                log.info("Processed uplink data: [{}]", uplinkData);
            }
        }
    }

    protected abstract class AbstractChannelHandler<T> extends SimpleChannelInboundHandler<T> {

        private Predicate<T> predicate;
        private Function<T, byte[]> transformer;

        protected AbstractChannelHandler(Function<T, byte[]> transformer, Predicate<T> predicate) {
            this.predicate = predicate;
            this.transformer = transformer;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
            try {
                if (!predicate.test(msg)) {
                    return;
                }
                process(integrationContext, new TcpipIntegrationMsg(transformer.apply(msg)));
            } catch (Exception e) {
                log.error("[{}] Text channel read Exception!", e.getMessage(), e);
                throw new Exception(e);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
            log.info("Message received on channel {}", ctx.name());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("Exception caught", cause);
        }
    }
}
