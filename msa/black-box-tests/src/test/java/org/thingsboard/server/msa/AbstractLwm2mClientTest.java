/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.common.util.JacksonUtil;
import org.eclipse.leshan.client.object.Security;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.msa.connectivity.lwm2m.LwM2MTestClient;
import org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.client.object.Security.psk;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_ENDPOINT_NO_SEC;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_ENDPOINT_PSK;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_LWM2M_SETTINGS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_PSK_IDENTITY;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.CLIENT_PSK_KEY;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.OBSERVE_ATTRIBUTES_WITHOUT_PARAMS;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.SECURE_URI;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.SECURITY_NO_SEC;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.resources;
import static org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mTestHelper.shortServerId;

@Slf4j
public abstract class AbstractLwm2mClientTest extends AbstractContainerTest{

    protected ScheduledExecutorService executor;

    protected Security security;

    protected LwM2MTestClient lwM2MTestClient;
    protected Device device;
    protected final PageLink pageLink = new PageLink(30);
    protected TenantId tenantId;

    protected DeviceProfile deviceProfile;
    protected String deviceProfileName = "Lwm2m_Profile";
    public final Set<LwM2MClientState> expectedStatusesRegistrationLwm2mSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));

    @BeforeMethod
    public void setUp() throws Exception {
        initTest();
    }

    @AfterMethod
    public void tearDown() {
        if (device != null) {
            testRestClient.deleteDeviceIfExists(device.getId());
        }
        if (deviceProfile != null) {
            testRestClient.deleteDeviceProfileIfExists(deviceProfile.getId());
        }
        clientDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void connectLwm2mClientNoSec() throws Exception {
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(CLIENT_ENDPOINT_NO_SEC));
        basicTestConnection(SECURITY_NO_SEC,
                deviceCredentials,
                CLIENT_ENDPOINT_NO_SEC);

    }
    public void connectLwm2mClientPsk() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK;
        String identity = CLIENT_PSK_IDENTITY;
        String keyPsk = CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security security = psk(SECURE_URI,
                shortServerId,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecurePsk(clientCredentials);
        basicTestConnection(security,
                deviceCredentials,
                clientEndpoint);
    }
    public void basicTestConnection(Security security,
                                    LwM2MDeviceCredentials deviceCredentials,
                                    String clientEndpoint) throws Exception {
        // create device
        device =  createDeviceWithCredentials(deviceCredentials, clientEndpoint);
        createNewClient(security, clientEndpoint, null, false);
        LwM2MClientState finishState =  ON_REGISTRATION_SUCCESS;
        await("")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTestConnection started -> finishState: [{}] states: {}", finishState, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(finishState) || lwM2MTestClient.getClientStates().contains(ON_REGISTRATION_STARTED);
                });
        await("")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    log.warn("basicTestConnection -> finishState: [{}] states: {}", finishState, lwM2MTestClient.getClientStates());
                    return lwM2MTestClient.getClientStates().contains(finishState) || lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS);
                });
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesRegistrationLwm2mSuccess));

    }

    public void createNewClient(Security security,
                                String endpoint, Integer clientDtlsCidLength, boolean queueMode) throws Exception {
        this.clientDestroy();
        lwM2MTestClient = new LwM2MTestClient(this.executor, endpoint);
        try (ServerSocket socket = new ServerSocket(0)) {
            int clientPort = socket.getLocalPort();
            lwM2MTestClient.init(security, clientPort, clientDtlsCidLength, queueMode);
        }
    }


    protected void clientDestroy() {
        try {
            if (lwM2MTestClient != null) {
                lwM2MTestClient.destroy();
            }
        } catch (Exception e) {
            log.error("Failed client Destroy", e);
        }
    }

    protected void initTest() throws Exception {
        executor = Executors.newScheduledThreadPool(10, ThingsBoardThreadFactory.forName("test-lwm2m-scheduled"));
        testRestClient.login("tenant@thingsboard.org", "tenant");
        deviceProfile = getDeviceProfile();
        tenantId = deviceProfile.getTenantId();
        for (String resourceName : resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwm2mClientTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(bytes);
            testRestClient.postTbResourceIfNotExists(lwModel);
        }
    }

    protected DeviceProfile getDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = getDeviceProfileIfExists();
        if (deviceProfile == null) {
            deviceProfile = testRestClient.postDeviceProfile(createDeviceProfile());
        }
        return deviceProfile;
    }

    protected DeviceProfile getDeviceProfileIfExists() throws Exception {
        return testRestClient.getDeviceProfiles(pageLink).getData().stream()
                .filter(x -> x.getName().equals(deviceProfileName))
                .findFirst()
                .orElse(null);
    }


    protected DeviceProfile createDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(deviceProfileName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(getTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    protected Device createDeviceWithCredentials(LwM2MDeviceCredentials deviceCredentials, String clientEndpoint) throws Exception {
        Device device = createDevice(deviceCredentials, clientEndpoint);
        return device;
    }

    protected Device createDevice(LwM2MDeviceCredentials credentials, String clientEndpoint) throws Exception {
        Device device = testRestClient.getDeviceByNameIfExists(clientEndpoint);
        if (device == null) {
            device = new Device();
            device.setName(clientEndpoint);
            device.setDeviceProfileId(deviceProfile.getId());
            device.setTenantId(tenantId);
            device = testRestClient.postDevice("", device);
        }

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        deviceCredentials = testRestClient.postDeviceCredentials(deviceCredentials);
        assertThat(deviceCredentials).isNotNull();
        return device;
    }

    protected LwM2MDeviceCredentials getDeviceCredentialsNoSec(LwM2MClientCredential clientCredentials) {
        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    public NoSecClientCredential createNoSecClientCredentials(String clientEndpoint) {
        NoSecClientCredential clientCredentials = new NoSecClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        return clientCredentials;
    }

    protected Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration() {
        List<LwM2MBootstrapServerCredential> bootstrapServerCredentials = new ArrayList<>();
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    protected LwM2MDeviceCredentials getDeviceCredentialsSecurePsk(LwM2MClientCredential clientCredentials) {
        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        LwM2MBootstrapClientCredentials bootstrapCredentials;
        bootstrapCredentials = getBootstrapClientCredentialsPsk(clientCredentials);
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    private LwM2MBootstrapClientCredentials getBootstrapClientCredentialsPsk(LwM2MClientCredential clientCredentials) {
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        PSKBootstrapClientCredential serverCredentials = new PSKBootstrapClientCredential();
        if (clientCredentials != null) {
            serverCredentials.setClientSecretKey(((PSKClientCredential) clientCredentials).getKey());
            serverCredentials.setClientPublicKeyOrId(((PSKClientCredential) clientCredentials).getIdentity());
        }
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        return bootstrapCredentials;
    }
}