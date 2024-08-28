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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.converter.Model;
import org.thingsboard.server.service.converter.Vendor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class ConverterLibraryControllerTest extends AbstractControllerTest {

    @Before
    public void before() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void test() throws Exception {
        String integrationType = "ChirpStack";

        List<Vendor> vendors = doGetTyped("/api/converter/library/" + integrationType + "/vendors", new TypeReference<>() {});
        assertThat(vendors).extracting(Vendor::name).contains("Milesight");

        for (Vendor vendor : vendors) {
            List<Model> models = doGetTyped("/api/converter/library/" + integrationType + "/" + vendor.name() + "/models", new TypeReference<>() {});
            assertThat(models).size().isGreaterThanOrEqualTo(8);
            assertThat(models).extracting(Model::name).contains("AT101", "WT201", "WS302");

            for (Model model : models) {
                ObjectNode uplinkConverter = doGet("/api/converter/library/" + integrationType + "/" + vendor.name() + "/" + model.name() + "/uplink", ObjectNode.class);
                assertThat(uplinkConverter.get("name").asText()).contains(model.name());
                assertThat(uplinkConverter.get("type").asText()).isEqualTo("UPLINK");
                assertThat(uplinkConverter.get("configuration").get("scriptLang").asText()).isEqualTo("TBEL");

                ObjectNode uplinkConverterMetadata = doGet("/api/converter/library/" + integrationType + "/" + vendor.name() + "/" + model.name() + "/uplink/metadata", ObjectNode.class);
                assertThat(uplinkConverterMetadata.get("integrationName").asText()).contains(integrationType);

                String uplinkPayload = doGet("/api/converter/library/" + integrationType + "/" + vendor.name() + "/" + model.name() + "/uplink/payload", String.class);
                assertThat(uplinkPayload).contains("\"applicationName\": \"Chirpstack application\"");


                if (!model.name().equals("WT201")) {
                    continue;
                }
                ObjectNode downlinkConverter = doGet("/api/converter/library/" + integrationType + "/" + vendor.name() + "/" + model.name() + "/downlink", ObjectNode.class);
                assertThat(downlinkConverter.get("name").asText()).contains(model.name());
                assertThat(downlinkConverter.get("type").asText()).isEqualTo("DOWNLINK");
                assertThat(downlinkConverter.get("configuration").get("scriptLang").asText()).isEqualTo("TBEL");

                ObjectNode downlinkConverterMetadata = doGet("/api/converter/library/" + integrationType + "/" + vendor.name() + "/" + model.name() + "/downlink/metadata", ObjectNode.class);
                assertThat(downlinkConverterMetadata.get("integrationName").asText()).contains(integrationType);

                String downlinkPayload = doGet("/api/converter/library/" + integrationType + "/" + vendor.name() + "/" + model.name() + "/downlink/payload", String.class);
                assertThat(downlinkPayload).contains("freeze_protection_config");
            }
        }
    }

}
