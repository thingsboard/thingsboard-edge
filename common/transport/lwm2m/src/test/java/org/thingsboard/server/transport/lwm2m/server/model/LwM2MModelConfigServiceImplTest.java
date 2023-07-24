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
package org.thingsboard.server.transport.lwm2m.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MModelConfigStore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

class LwM2MModelConfigServiceImplTest {

    LwM2MModelConfigServiceImpl service;
    TbLwM2MModelConfigStore modelStore;

    @BeforeEach
    void setUp() {
        service = new LwM2MModelConfigServiceImpl();
        modelStore = mock(TbLwM2MModelConfigStore.class);
        service.modelStore = modelStore;
    }

    @Test
    void testInitWithDuplicatedModels() {
        LwM2MModelConfig config = new LwM2MModelConfig("urn:imei:951358811362976");
        List<LwM2MModelConfig> models = List.of(config, config);
        willReturn(models).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).containsExactlyEntriesOf(Map.of(config.getEndpoint(), config));
    }

    @Test
    void testInitWithNonUniqueEndpoints() {
        LwM2MModelConfig configAlfa = new LwM2MModelConfig("urn:imei:951358811362976");
        LwM2MModelConfig configBravo = new LwM2MModelConfig("urn:imei:151358811362976");
        LwM2MModelConfig configDelta = new LwM2MModelConfig("urn:imei:151358811362976");
        assertThat(configBravo.getEndpoint()).as("non-unique endpoints provided").isEqualTo(configDelta.getEndpoint());
        List<LwM2MModelConfig> models = List.of(configAlfa, configBravo, configDelta);
        willReturn(models).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).containsExactlyInAnyOrderEntriesOf(Map.of(
                configAlfa.getEndpoint(), configAlfa,
                configBravo.getEndpoint(), configBravo
        ));
    }

    @Test
    void testInitWithEmptyModels() {
        willReturn(Collections.emptyList()).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).isEmpty();
    }

}
