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
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.*;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.converter.UplinkData;
import org.thingsboard.server.service.converter.UplinkMetaData;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
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

    private OpcUaServerConfiguration opcUaServerConfiguration;

    private IntegrationContext integrationContext;
    private OpcUaClient client;
    private UaSubscription subscription;
    private Map<NodeId, OpcUaDevice> devices;
    private Map<NodeId, List<OpcUaDevice>> devicesByTags;
    private Map<Pattern, DeviceMapping> mappings;
    private ScheduledExecutorService executor;

    private final AtomicLong clientHandles = new AtomicLong(1L);
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
        this.devices = new HashMap<>();
        this.devicesByTags = new HashMap<>();
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
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.warn("Failed to apply data converter function", e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), mapper.writeValueAsString(msg.toJson()), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message", e);
            }
        }
    }

    private void doProcess(IntegrationContext context, OpcUaIntegrationMsg msg) throws Exception {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        mdMap.putAll(msg.getDeviceMetadata());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, msg.getPayload(), new UplinkMetaData(getUplinkContentType(), mdMap));
        if (uplinkDataList != null) {
            for (UplinkData data : uplinkDataList) {
                processUplinkData(context, data);
                log.info("[{}] Processing uplink data: {}", configuration.getId(), data);
            }
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

            subscription = client.getSubscriptionManager().createSubscription(1000.0).get();

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
                client.disconnect().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {}
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public void scanForDevices() {
        try {
            long startTs = System.currentTimeMillis();
            scanForDevices(new OpcUaNode(Identifiers.RootFolder, ""));
            log.info("Device scan cycle completed in {} ms", (System.currentTimeMillis() - startTs));
            List<OpcUaDevice> deleted = devices.entrySet().stream().filter(kv -> kv.getValue().getScanTs() < startTs).map(kv -> kv.getValue()).collect(Collectors.toList());
            if (deleted.size() > 0) {
                log.info("Devices {} are no longer available", deleted);
            }
            deleted.forEach(devices::remove);
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
                scanDevice(node, m);
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

    private void scanDevice(OpcUaNode node, DeviceMapping m) throws ExecutionException, InterruptedException {
        log.debug("Scanning device node: {}", node);
        Set<String> tags = m.getAllTags();
        log.debug("Scanning node hierarchy for tags: {}", tags);
        Map<String, NodeId> tagMap = lookupTags(node.getNodeId(), node.getName(), tags);
        log.debug("Scanned {} tags out of {}", tagMap.size(), tags.size());

        OpcUaDevice device;
        if (devices.containsKey(node.getNodeId())) {
            device = devices.get(node.getNodeId());
        } else {
            device = new OpcUaDevice(node, m);
            devices.put(node.getNodeId(), device);
            Map<String, NodeId> newTags = device.registerTags(tagMap);
            if (newTags.size() > 0) {
                for (NodeId tagId : newTags.values()) {
                    devicesByTags.computeIfAbsent(tagId, key -> new ArrayList<>()).add(device);
                    VariableNode varNode = client.getAddressSpace().createVariableNode(tagId);
                    DataValue dataValue = varNode.readValue().get();
                    if (dataValue != null) {
                        device.updateTag(tagId, dataValue);
                    }
                }
                log.debug("Going to subscribe to tags: {}", newTags);
                subscribeToTags(newTags);
            }
            onDeviceDataUpdate(device, null);
        }

        device.updateScanTs();

        Map<String, NodeId> newTags = device.registerTags(tagMap);
        if (newTags.size() > 0) {
            for (NodeId tagId : newTags.values()) {
                devicesByTags.computeIfAbsent(tagId, key -> new ArrayList<>()).add(device);
            }
            log.debug("Going to subscribe to tags: {}", newTags);
            subscribeToTags(newTags);
        }
    }

    private void onDeviceDataUpdate(OpcUaDevice device, NodeId affectedTagId) {
        OpcUaIntegrationMsg message = device.prepareMsg(affectedTagId);
        process(integrationContext, message);
    }

    private void subscribeToTags(Map<String, NodeId> newTags) throws InterruptedException, ExecutionException {
        List<MonitoredItemCreateRequest> requests = new ArrayList<>();
        for (Map.Entry<String, NodeId> kv : newTags.entrySet()) {
            // subscribe to the Value attribute of the server's CurrentTime node
            ReadValueId readValueId = new ReadValueId(
                    kv.getValue(),
                    AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
            // important: client handle must be unique per item
            UInteger clientHandle = uint(clientHandles.getAndIncrement());

            MonitoringParameters parameters = new MonitoringParameters(
                    clientHandle,
                    1000.0,     // sampling interval
                    null,       // filter, null means use default
                    uint(10),   // queue size
                    true        // discard oldest
            );

            requests.add(new MonitoredItemCreateRequest(
                    readValueId, MonitoringMode.Reporting, parameters));
        }

        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
                (item, id) -> item.setValueConsumer(this::onSubscriptionValue);

        List<UaMonitoredItem> items = subscription.createMonitoredItems(
                TimestampsToReturn.Both,
                requests,
                onItemCreated
        ).get();

        for (UaMonitoredItem item : items) {
            if (item.getStatusCode().isGood()) {
                log.trace("Monitoring Item created for nodeId={}", item.getReadValueId().getNodeId());
            } else {
                log.warn("Failed to create item for nodeId={} (status={})",
                        item.getReadValueId().getNodeId(), item.getStatusCode());
            }
        }
    }

    private void onSubscriptionValue(UaMonitoredItem item, DataValue dataValue) {
        log.debug("Subscription value received: item={}, value={}",
                item.getReadValueId().getNodeId(), dataValue.getValue());
        NodeId tagId = item.getReadValueId().getNodeId();
        devicesByTags.getOrDefault(tagId, Collections.emptyList()).forEach(
                device -> {
                    device.updateTag(tagId, dataValue);
                    onDeviceDataUpdate(device, tagId);
                }
        );
    }

    private Map<String, NodeId> lookupTags(NodeId nodeId, String deviceNodeName, Set<String> tags) {
        Map<String, NodeId> values = new HashMap<>();
        try {
            BrowseResult browseResult = client.browse(getBrowseDescription(nodeId)).get();
            List<ReferenceDescription> references = toList(browseResult.getReferences());

            for (ReferenceDescription rd : references) {
                NodeId childId;
                if (rd.getNodeId().isLocal()) {
                    childId = rd.getNodeId().local().get();
                } else {
                    log.trace("Ignoring remote node: {}", rd.getNodeId());
                    continue;
                }

                String name;
                String childIdStr = childId.getIdentifier().toString();
                if (childIdStr.contains(deviceNodeName)) {
                    name = childIdStr.substring(childIdStr.indexOf(deviceNodeName) + deviceNodeName.length() + 1, childIdStr.length());
                } else {
                    name = rd.getBrowseName().getName();
                }
                if (tags.contains(name)) {
                    values.put(name, childId);
                }
                // recursively browse to children
                values.putAll(lookupTags(childId, deviceNodeName, tags));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Browsing nodeId={} failed: {}", nodeId, e.getMessage(), e);
        }
        return values;
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
