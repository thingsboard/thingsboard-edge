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
package org.thingsboard.server.transport.lwm2m.transportConfiguration;

import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_ALL;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_BY_OBJECT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class ObserveStrategyWithNoSecQueueModeConnectTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveSingleTelemetryUpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveSingle";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        super.basicTestConnectionObserveSingleTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, true, true);
    }

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveCompositeAllTelemetry_Both_UpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveCompositeAll";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = super.getTransportConfiguration(TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3, getBootstrapServerCredentialsNoSec(NONE));
        transportConfiguration.getObserveAttr().setObserveStrategy(COMPOSITE_ALL);
        super.basicTestConnectionObserveCompositeTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, transportConfiguration, 1, 0);
    }

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveCompositeByObjectTelemetry_Both_UpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveCompositeByObject";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = super.getTransportConfiguration(TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3, getBootstrapServerCredentialsNoSec(NONE));
        transportConfiguration.getObserveAttr().setObserveStrategy(COMPOSITE_BY_OBJECT);
        super.basicTestConnectionObserveCompositeTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, transportConfiguration, 2, 1);
    }

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveCompositeByObjectTelemetry_Single_UpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveCompositeByObject_Single";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = super.getTransportConfiguration(TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3, getBootstrapServerCredentialsNoSec(NONE));
        transportConfiguration.getObserveAttr().setObserveStrategy(COMPOSITE_BY_OBJECT);
        super.basicTestConnectionObserveCompositeTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, transportConfiguration, 2, 2);
    }
}

