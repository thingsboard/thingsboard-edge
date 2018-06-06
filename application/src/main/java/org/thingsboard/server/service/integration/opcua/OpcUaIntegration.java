/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.service.integration.opcua;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseResultMask;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.toList;

/**
 * Created by Valerii Sosliuk on 3/17/2018.
 */
@Slf4j
public class OpcUaIntegration extends AbstractIntegration<OpcUaIntegrationMsg> {

    private Map<Pattern, DeviceMapping> mappings;
    private OpcUaClient client;
    private ScheduledExecutorService executor;
    private OpcUaServerConfiguration opcUaServerConfiguration;
    private IntegrationContext integrationContext;
    private volatile boolean connected = false;

    public OpcUaIntegration(IntegrationContext integrationContext) {
        this.integrationContext = integrationContext;
    }

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        opcUaServerConfiguration = mapper.readValue(
                mapper.writeValueAsString(configuration.getConfiguration().get("clientConfiguration")),
                OpcUaServerConfiguration.class);
        if (opcUaServerConfiguration.getMapping().isEmpty()) {
            throw new IllegalArgumentException("No mapping elements configured!");
        }
        this.mappings = opcUaServerConfiguration.getMapping().stream().collect(Collectors.toMap(m -> Pattern.compile(m.getDeviceNodePattern()), Function.identity()));
        initClient(opcUaServerConfiguration);
        connected = true;
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.execute(() -> scanForDevices());
    }

    @Override
    public void update(TbIntegrationInitParams params) throws Exception {
        destroy();
        init(params);
    }

    @Override
    public void process(IntegrationContext context, OpcUaIntegrationMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        try {
            List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData(getUplinkContentType(), mdMap));
            if (uplinkDataList != null) {
                for (UplinkData data : uplinkDataList) {
                    processUplinkData(context, data);
                    log.info("[{}] Processing uplink data: {}", configuration.getId(), data);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to procss OPC-UA device [" + msg.getNodeId() + "]. Reason: " + e.getMessage(), e);
        }
    }

    private void initClient(OpcUaServerConfiguration configuration) throws Exception {
        try {

            log.info("Initializing OPC-UA server connection to [{}:{}]!", configuration.getHost(), configuration.getPort());

            SecurityPolicy securityPolicy = SecurityPolicy.valueOf(configuration.getSecurity());
            IdentityProvider identityProvider = configuration.getIdentity().toProvider();

            EndpointDescription[] endpoints;
            String endpointUrl = "opc.tcp://" + configuration.getHost() + ":" + configuration.getPort();
            try {
                endpoints = UaTcpStackClient.getEndpoints(endpointUrl).get();
            } catch (ExecutionException e) {
                log.error("Failed to connect to provided endpoint!", e);
                throw new RuntimeException("Failed to connect to provided endpoint: " + endpointUrl);
            }

            EndpointDescription endpoint = Arrays.stream(endpoints)
                    .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri()))
                    .findFirst().orElseThrow(() -> new Exception("no desired endpoints returned"));

            OpcUaClientConfigBuilder configBuilder = OpcUaClientConfig.builder()
                    .setApplicationName(LocalizedText.english(configuration.getApplicationName()))
                    .setApplicationUri(configuration.getApplicationUri())
                    .setEndpoint(endpoint)
                    .setIdentityProvider(identityProvider)
                    .setRequestTimeout(uint(configuration.getTimeoutInMillis()));

            if (securityPolicy != SecurityPolicy.None) {
                CertificateInfo certificate = OpcUaConfigurationTools.loadCertificate(configuration.getKeystore());
                configBuilder.setCertificate(certificate.getCertificate())
                        .setKeyPair(certificate.getKeyPair());
            }

            OpcUaClientConfig config = configBuilder.build();

            client = new OpcUaClient(config);
            client.connect().get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            log.error("Failed to connect to OPC-UA server. Reason: {}", t.getMessage(), t);
            throw new RuntimeException("Failed to connect to OPC-UA server. Reason: " + t.getMessage());
        }

    }

    @Override
    public void destroy() {
        if (connected) {
            try {
                connected = false;
                client.disconnect().get(2, TimeUnit.SECONDS);
            } catch (Exception e) {}
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void scanForDevices() {
        try {
            long startTs = System.currentTimeMillis();
            OpcUaNode rootNode = new OpcUaNode(Identifiers.RootFolder, "");
            scanForDevices(rootNode);
            log.info("Device scan cycle completed in {} ms", (System.currentTimeMillis() - startTs));
        } catch (Exception e) {
            log.warn("Periodic device scan failed!", e);
        }
        if (connected) {
            log.info("Scheduling next scan in {} seconds!", opcUaServerConfiguration.getScanPeriodInSeconds());
            executor.schedule(() -> {
                scanForDevices();
            }, opcUaServerConfiguration.getScanPeriodInSeconds(), TimeUnit.SECONDS);
        }
    }

    private void scanForDevices(OpcUaNode node) {
        log.trace("Scanning node: {}", node);


        List<DeviceMapping> matchedMappings = mappings.entrySet().stream()
                .filter(mappingEntry -> mappingEntry.getKey().matcher(getMatchingValue(node, mappingEntry.getValue().getMappingType())).matches())
                .map(m -> m.getValue()).collect(Collectors.toList());

        matchedMappings.forEach(m -> {
            try {
                log.debug("Matched mapping: [{}]", m);
                scanDevice(node);
            } catch (Exception e) {
                log.error("Failed to scan device: {}", node, e);
            }
        });

        try {
            BrowseResult browseResult = client.browse(getBrowseDescription(node.getNodeId())).get();
            List<ReferenceDescription> references = toList(browseResult.getReferences());
            for (ReferenceDescription rd : references) {
                if (rd.getNodeId().isLocal()) {
                    NodeId childNodeId = rd.getNodeId().local().get();
                    OpcUaNode childNode = new OpcUaNode(node, childNodeId, rd.getBrowseName().getName());
                    scanForDevices(childNode);
                } else {
                    log.trace("Ignoring remote node: {}", rd.getNodeId());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (connected) {
                log.error("Browsing nodeId={} failed: {}", node, e.getMessage(), e);
            }
        }
    }

    private String getMatchingValue(OpcUaNode node, DeviceMappingType mappingType) {
        switch (mappingType) {
            case ID : return node.getNodeId().getIdentifier().toString();
            case FQN : return node.getFqn();
            default: throw new IllegalArgumentException("unknown DeviceMappingType: " + mappingType);
        }
    }

    private void scanDevice(OpcUaNode node) throws ExecutionException, InterruptedException {
        log.debug("Scanning device node: {}", node);
        BrowseResult browseResult = client.browse(getBrowseDescription(node.getNodeId())).get();
        List<ReferenceDescription> references = toList(browseResult.getReferences());

        ObjectNode dataNode = mapper.createObjectNode();

        ObjectNode nodeId = mapper.createObjectNode();
        nodeId.put("identifier", node.getNodeId().getIdentifier().toString());
        nodeId.put("namespaceIndex", node.getNodeId().getNamespaceIndex().toString());

        dataNode.set("nodeId", nodeId);
        dataNode.put("name", node.getName());
        dataNode.put("fqn", node.getFqn());

        for (ReferenceDescription rd : references) {
            if (rd.getNodeId().isLocal()) {
                NodeId childId = rd.getNodeId().local().get();
                VariableNode varNode = client.getAddressSpace().createVariableNode(childId);
                ObjectNode entryNode = mapper.createObjectNode();
                ObjectNode referenceNode = getReferenceNode(rd);
                entryNode.set("referenceDescription", referenceNode);

                DataValue data = varNode.readValue().get();
                if (data != null) {
                    ObjectNode dataValNode = getDataValNode(data);

                    entryNode.set("dataValue", dataValNode);
                }
                dataNode.set(rd.getBrowseName().getName(), entryNode);
            } else {
                log.info("Remote node. Skipping for now: [] ", rd.getNodeId());
            }
        }

        process(integrationContext, new OpcUaIntegrationMsg(dataNode));
        log.trace("Scanned device: ", node);
    }

    private ObjectNode getReferenceNode(ReferenceDescription rd) {
        ObjectNode referenceNode = mapper.createObjectNode();

        if (rd.getReferenceTypeId() != null) {
            ObjectNode referenceTypeId = mapper.createObjectNode();
            referenceTypeId.put("identifier", rd.getReferenceTypeId().getIdentifier().toString());
            referenceTypeId.put("namespaceIndex", rd.getReferenceTypeId().getNamespaceIndex().toString());
            referenceNode.set("_referenceTypeId", referenceTypeId);
        }
        referenceNode.put("_isForward", rd.getIsForward());

        if (rd.getNodeId() != null) {
            ObjectNode nodeId = mapper.createObjectNode();
            nodeId.put("identifier", rd.getNodeId().getIdentifier().toString());
            nodeId.put("namespaceIndex", rd.getNodeId().getNamespaceIndex() == null ? null : rd.getNodeId().getNamespaceIndex().toString());
            nodeId.put("namespaceIndex", rd.getNodeId().getNamespaceIndex() == null ? null : rd.getNodeId().getNamespaceIndex().toString());
            nodeId.put("namespaceUri", rd.getNodeId().getNamespaceUri() == null ? null : rd.getNodeId().getNamespaceUri().toString());
            nodeId.put("serverIndex", rd.getNodeId().getServerIndex());
            nodeId.put("type", rd.getNodeId().getType().toString());
            referenceNode.set("_nodeId", nodeId);
        }

        if (rd.getBrowseName() != null) {
            ObjectNode browseName = mapper.createObjectNode();
            browseName.put("namespaceIndex", rd.getBrowseName().getNamespaceIndex() == null ? null : rd.getBrowseName().getNamespaceIndex().toString());
            browseName.put("name", rd.getBrowseName().getName());
            referenceNode.set("_browseName", browseName);
        }
        if (rd.getDisplayName() != null) {
            ObjectNode displayName = mapper.createObjectNode();
            displayName.put("namespaceIndex", rd.getBrowseName() == null ? null : rd.getBrowseName().getNamespaceIndex().toString());
            displayName.put("name", rd.getBrowseName().getName());
            referenceNode.set("_displayName", displayName);
        }
        if (rd.getNodeClass() != null) {
            referenceNode.put("_nodeClass", rd.getNodeClass().toString());
        }
        if (rd.getTypeDefinition() != null) {
            ObjectNode nodeId = mapper.createObjectNode();
            nodeId.put("identifier", rd.getTypeDefinition().getIdentifier() == null ? null : rd.getTypeDefinition().getIdentifier().toString());
            nodeId.put("namespaceIndex", rd.getTypeDefinition().getNamespaceIndex() == null ? null : rd.getTypeDefinition().getNamespaceIndex().toString());
            nodeId.put("namespaceUri", rd.getTypeDefinition().getNamespaceUri() == null ? null : rd.getTypeDefinition().getNamespaceUri().toString());
            nodeId.put("serverIndex", rd.getTypeDefinition().getServerIndex());
            nodeId.put("type", rd.getTypeDefinition().getType() == null ? null : rd.getTypeDefinition().getType().toString());
            referenceNode.set("_typeDefinition", nodeId);
        }
        return referenceNode;
    }

    private ObjectNode getDataValNode(DataValue data) {
        ObjectNode dataValNode = mapper.createObjectNode();
        if (data.getValue() != null) {
            dataValNode.put("value", data.getValue().getValue() == null ? null : data.getValue().getValue().toString());
        }
        if (data.getStatusCode() != null) {
            dataValNode.put("statusCode", data.getStatusCode().toString());
        }
        dataValNode.put("sourceTime", data.getSourceTime() == null ? null : data.getSourceTime().getJavaTime());
        dataValNode.put("sourcePicoseconds", data.getSourcePicoseconds() == null ? null : data.getSourcePicoseconds().doubleValue());
        dataValNode.put("serverTime", data.getServerTime() == null ? null : data.getServerTime().getJavaTime());
        dataValNode.put("serverPicoseconds", data.getServerPicoseconds() == null ? null : data.getServerPicoseconds().doubleValue());
        return dataValNode;
    }

    private BrowseDescription getBrowseDescription(NodeId nodeId) {
        return new BrowseDescription(
                nodeId,
                BrowseDirection.Forward,
                Identifiers.References,
                true,
                uint(NodeClass.Object.getValue() | NodeClass.Variable.getValue()),
                uint(BrowseResultMask.All.getValue())
        );
    }
}
