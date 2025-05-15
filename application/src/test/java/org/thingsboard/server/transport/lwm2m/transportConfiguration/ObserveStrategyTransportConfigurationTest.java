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

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_ALL;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class ObserveStrategyTransportConfigurationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testTransportConfigurationObserveStrategyBeforeParseNullAfterParseNotNull_STRATEGY_SINGLE() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITHOUT_OBSERVE, getBootstrapServerCredentialsNoSec(NONE));
        Assert.assertNotNull(transportConfiguration.getObserveAttr().getObserveStrategy());
        Assert.assertEquals(SINGLE, transportConfiguration.getObserveAttr().getObserveStrategy());
    }

    @Test
    public void testTransportConfigurationObserveStrategyBeforeParseNotNullAfterParseNotNull_STRATEGY_COMPOSITE_ALL() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_COMPOSITE_ALL_OBSERVE_ID_3_ID_19, getBootstrapServerCredentialsNoSec(NONE));
        Assert.assertNotNull(transportConfiguration.getObserveAttr().getObserveStrategy());
        Assert.assertEquals(COMPOSITE_ALL, transportConfiguration.getObserveAttr().getObserveStrategy());
    }
}
