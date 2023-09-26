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
package org.thingsboard.server.system;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Andrew Shvayka
 */
@TestPropertySource(properties = {
        "transport.http.enabled=true",
})
public abstract class BaseHttpDeviceApiTest extends AbstractControllerTest {

    private static final AtomicInteger idSeq = new AtomicInteger(new Random(System.currentTimeMillis()).nextInt());

    protected Device device;
    protected DeviceCredentials deviceCredentials;

    @Before
    public void before() throws Exception {
        loginTenantAdmin();
        device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
    }

    @Test
    public void testGetAttributes() throws Exception {
        doGetAsync("/api/v1/" + "WRONG_TOKEN" + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isUnauthorized());
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isOk());

        Map<String, String> attrMap = new HashMap<>();
        attrMap.put("keyA", "valueA");
        mockMvc.perform(
                asyncDispatch(doPost("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes", attrMap, new String[]{}).andReturn()))
                .andExpect(status().isOk());
        Thread.sleep(2000);
        doGetAsync("/api/v1/" + deviceCredentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC").andExpect(status().isOk());
    }

    protected ResultActions doGetAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest;
        getRequest = get(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

    protected ResultActions doPostAsync(String urlTemplate, Object... urlVariables) throws Exception {
        MockHttpServletRequestBuilder getRequest = post(urlTemplate, urlVariables);
        setJwtToken(getRequest);
        return mockMvc.perform(asyncDispatch(mockMvc.perform(getRequest).andExpect(request().asyncStarted()).andReturn()));
    }

}
