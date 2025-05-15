/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.californium.endpoint.ClientProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coap.CoapOscoreProtocolProvider;
import org.eclipse.leshan.client.californium.endpoint.coaps.CoapsClientProtocolProvider;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.engine.DefaultRegistrationEngineFactory;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.observer.LwM2mClientObserver;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.DummyInstanceEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.thingsboard.monitoring.util.ResourceUtils;

import javax.security.auth.Destroyable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        Configuration coapConfig = new Configuration();
        String portStr = StringUtils.substringAfterLast(serverUri, ":");
        if (StringUtils.isNotEmpty(portStr)) {
            coapConfig.set(CoapConfig.COAP_PORT, Integer.parseInt(portStr));
        }

        LwM2mModel model = new StaticModel(models);
        ObjectsInitializer initializer = new ObjectsInitializer(model);
        initializer.setInstancesForObject(SECURITY, security);
        initializer.setInstancesForObject(SERVER, new Server(123, TimeUnit.MINUTES.toSeconds(5)));
        initializer.setInstancesForObject(DEVICE, this);
        initializer.setClassForObject(ACCESS_CONTROL, DummyInstanceEnabler.class);

        // Create client endpoints Provider
        List<ClientProtocolProvider> protocolProvider = new ArrayList<>();
        protocolProvider.add(new CoapOscoreProtocolProvider());
        protocolProvider.add(new CoapsClientProtocolProvider());
        CaliforniumClientEndpointsProvider.Builder endpointsBuilder = new CaliforniumClientEndpointsProvider.Builder(
                protocolProvider.toArray(new ClientProtocolProvider[protocolProvider.size()]));

        // Create Californium Configuration
        Configuration clientCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        clientCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        clientCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, true);

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(clientCoapConfig);

        // creates EndpointsProvider
        List<LwM2mClientEndpointsProvider> endpointsProvider = new ArrayList<>();
        endpointsProvider.add(endpointsBuilder.build());

        // Configure registration engine
        DefaultRegistrationEngineFactory engineFactory = new DefaultRegistrationEngineFactory();
        engineFactory.setReconnectOnUpdate(false);
        engineFactory.setResumeOnConnect(true);

        // Build the client
        LeshanClientBuilder builder = new LeshanClientBuilder(endpoint);
        builder.setObjects(initializer.createAll());
        builder.setEndpointsProviders(endpointsProvider.toArray(new LwM2mClientEndpointsProvider[endpointsProvider.size()]));
        builder.setRegistrationEngineFactory(engineFactory);
        builder.setDecoder(new DefaultLwM2mDecoder(false));
        builder.setEncoder(new DefaultLwM2mEncoder(false));
        leshanClient = builder.build();

        // Add observer
        LwM2mClientObserver observer = new LwM2mClientObserver() {
            @Override
            public void onBootstrapStarted(LwM2mServer bsserver, BootstrapRequest request) {
                // No implementation needed
            }

            @Override
            public void onBootstrapSuccess(LwM2mServer bsserver, BootstrapRequest request) {
                // No implementation needed
            }

            @Override
            public void onBootstrapFailure(LwM2mServer bsserver, BootstrapRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                // No implementation needed
            }

            @Override
            public void onBootstrapTimeout(LwM2mServer bsserver, BootstrapRequest request) {
                // No implementation needed
            }

            @Override
            public void onRegistrationStarted(LwM2mServer server, RegisterRequest request) {
                log.debug("onRegistrationStarted [{}]", request.getEndpointName());
            }

            @Override
            public void onRegistrationSuccess(LwM2mServer server, RegisterRequest request, String registrationID) {
                log.debug("onRegistrationSuccess [{}] [{}]", request.getEndpointName(), registrationID);
            }

            @Override
            public void onRegistrationFailure(LwM2mServer server, RegisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.debug("onRegistrationFailure [{}] [{}] [{}]", request.getEndpointName(), responseCode, errorMessage);
            }

            @Override
            public void onRegistrationTimeout(LwM2mServer server, RegisterRequest request) {
                log.debug("onRegistrationTimeout [{}]", request.getEndpointName());
            }

            @Override
            public void onUpdateStarted(LwM2mServer server, UpdateRequest request) {
                log.debug("onUpdateStarted [{}]", request.getRegistrationId());
            }

            @Override
            public void onUpdateSuccess(LwM2mServer server, UpdateRequest request) {
                log.debug("onUpdateSuccess [{}]", request.getRegistrationId());
            }

            @Override
            public void onUpdateFailure(LwM2mServer server, UpdateRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.debug("onUpdateFailure [{}]", request.getRegistrationId());
            }

            @Override
            public void onUpdateTimeout(LwM2mServer server, UpdateRequest request) {
                log.debug("onUpdateTimeout [{}]", request.getRegistrationId());
            }

            @Override
            public void onDeregistrationStarted(LwM2mServer server, DeregisterRequest request) {
                log.debug("onDeregistrationStarted [{}]", request.getRegistrationId());
            }

            @Override
            public void onDeregistrationSuccess(LwM2mServer server, DeregisterRequest request) {
                log.debug("onDeregistrationSuccess [{}]", request.getRegistrationId());
            }

            @Override
            public void onDeregistrationFailure(LwM2mServer server, DeregisterRequest request, ResponseCode responseCode, String errorMessage, Exception cause) {
                log.debug("onDeregistrationFailure [{}] [{}] [{}]", request.getRegistrationId(), responseCode, errorMessage);
            }

            @Override
            public void onDeregistrationTimeout(LwM2mServer server, DeregisterRequest request) {
                log.debug("onDeregistrationTimeout [{}]", request.getRegistrationId());
            }

            @Override
            public void onUnexpectedError(Throwable unexpectedError) {
                log.debug("onUnexpectedError [{}]", unexpectedError.toString());
            }
        };
        leshanClient.addObserver(observer);

        setLeshanClient(leshanClient);

        leshanClient.start();
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public ReadResponse read(LwM2mServer server, int resourceId) {
        if (supportedResources.contains(resourceId)) {
            return ReadResponse.success(resourceId, data);
        }
        return super.read(server, resourceId);
    }

    @SneakyThrows
    public void send(String data, int resource) {
        this.data = data;
        fireResourceChange(resource);
    }

    @Override
    public void destroy() {
        if (leshanClient != null) {
            leshanClient.destroy(true);
        }
    }
}
