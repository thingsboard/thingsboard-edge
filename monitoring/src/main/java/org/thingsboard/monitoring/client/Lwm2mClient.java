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
package org.thingsboard.monitoring.client;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.observe.ObservationStore;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.EndpointFactory;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.monitoring.util.ResourceUtils;

import javax.security.auth.Destroyable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;

@Slf4j
public class Lwm2mClient extends BaseInstanceEnabler implements Destroyable {

    @Getter
    @Setter
    private LeshanClient leshanClient;

    private static final List<Integer> supportedResources = Collections.singletonList(0);

    private String data = "";

    private String serverUri;
    private String endpoint;

    public Lwm2mClient(String serverUri, String endpoint) {
        this.serverUri = serverUri;
        this.endpoint = endpoint;
    }

    public Lwm2mClient() {
    }

    public void initClient() throws InvalidDDFFileException, IOException {
        String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "test-model.xml"};
        List<ObjectModel> models = new ArrayList<>();
        for (String resourceName : resources) {
            models.addAll(ObjectLoader.loadDdfFile(ResourceUtils.getResourceAsStream("lwm2m/models/" + resourceName), resourceName));
        }

        Security security = noSec(serverUri, 123);
        NetworkConfig coapConfig = new NetworkConfig().setString(NetworkConfig.Keys.COAP_PORT, StringUtils.substringAfterLast(serverUri, ":"));

        LeshanClient leshanClient;

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, security);
        initializer.setInstancesForObject(SERVER, new Server(123, 300));
        initializer.setInstancesForObject(DEVICE, this);
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(true);
        dtlsConfig.setClientOnly();

        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setReconnectOnUpdate(false);
        engineFactory.setResumeOnConnect(true);

        EndpointFactory endpointFactory = new EndpointFactory() {

            @Override
            public CoapEndpoint createUnsecuredEndpoint(InetSocketAddress address, NetworkConfig coapConfig,
                                                        ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                builder.setInetSocketAddress(address);
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }

            @Override
            public CoapEndpoint createSecuredEndpoint(DtlsConnectorConfig dtlsConfig, NetworkConfig coapConfig,
                                                      ObservationStore store) {
                CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
                DtlsConnectorConfig.Builder dtlsConfigBuilder = new DtlsConnectorConfig.Builder(dtlsConfig);
                builder.setConnector(new DTLSConnector(dtlsConfigBuilder.build()));
                builder.setNetworkConfig(coapConfig);
                return builder.build();
            }
        };

        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(initializer.createAll());
        builder.setCoapConfig(coapConfig);
        builder.setDtlsConfig(dtlsConfig);
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setEndpointFactory(endpointFactory);
        builder.setDecoder(new DefaultLwM2mDecoder(false));
        builder.setEncoder(new DefaultLwM2mEncoder(false));
        leshanClient = builder.build();

        setLeshanClient(leshanClient);

        leshanClient.start();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceId) {
        if (supportedResources.contains(resourceId)) {
            return ReadResponse.success(resourceId, data);
        }
        return super.read(identity, resourceId);
    }

    @SneakyThrows
    public void send(String data, int resource) {
        this.data = data;
        fireResourcesChange(resource);
    }

    @Override
    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
        }
    }
}
