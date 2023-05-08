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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;

@ActiveProfiles("install")
@SpringBootTest(classes = DefaultDataUpdateService.class)
class DefaultDataUpdateServiceTest {

    ObjectMapper mapper = new ObjectMapper();

    @MockBean
    DefaultDataUpdateService service;

    @BeforeEach
    void setUp() {
        willCallRealMethod().given(service).convertDeviceProfileAlarmRulesForVersion330(any());
        willCallRealMethod().given(service).convertDeviceProfileForVersion330(any());
    }

    JsonNode readFromResource(String resourceName) throws IOException {
        return mapper.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330FirstRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_in.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isTrue();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330SecondRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_out.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330EmptyJson() throws JsonProcessingException {
        JsonNode spec = mapper.readTree("{ }");
        JsonNode expected = mapper.readTree("{ }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330AlarmNodeNull() throws JsonProcessingException {
        JsonNode spec = mapper.readTree("{ \"alarms\" : null }");
        JsonNode expected = mapper.readTree("{ \"alarms\" : null }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    @Test
    void convertDeviceProfileAlarmRulesForVersion330NoAlarmNode() throws JsonProcessingException {
        JsonNode spec = mapper.readTree("{ \"configuration\": { \"type\": \"DEFAULT\" } }");
        JsonNode expected = mapper.readTree("{ \"configuration\": { \"type\": \"DEFAULT\" } }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

}
