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
package org.thingsboard.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CalculatedFieldEdgeTest extends AbstractEdgeTest {
    private static final String DEFAULT_CF_NAME = "Edge Test CalculatedField";
    private static final String UPDATED_CF_NAME = "Updated Edge Test CalculatedField";

    @Test
    @Ignore
    public void testCalculatedField_create_update_delete() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);

        edgeImitator.expectMessageAmount(SYNC_MESSAGE_COUNT + 4);
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);
        doPost("/api/edge/sync/" + edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        List<AbstractMessage> downlinkMsgs = edgeImitator.getDownlinkMsgs();
        AbstractMessage latestMessage = downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg instanceof CalculatedFieldUpdateMsg).findFirst().get();
        Assert.assertTrue(latestMessage instanceof CalculatedFieldUpdateMsg);
        CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = (CalculatedFieldUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, calculatedFieldUpdateMsg.getMsgType());
        Assert.assertEquals(savedCalculatedField.getUuidId().getMostSignificantBits(), calculatedFieldUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCalculatedField.getUuidId().getLeastSignificantBits(), calculatedFieldUpdateMsg.getIdLSB());
        CalculatedField calculatedFieldFromMsg = JacksonUtil.fromString(calculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
        Assert.assertNotNull(calculatedFieldFromMsg);

        Assert.assertEquals(DEFAULT_CF_NAME, calculatedFieldFromMsg.getName());
        Assert.assertEquals(savedDevice.getId(), calculatedFieldFromMsg.getEntityId());
        Assert.assertEquals(config, calculatedFieldFromMsg.getConfiguration());

        // update calculatedField
        edgeImitator.expectMessageAmount(SYNC_MESSAGE_COUNT + 4);
        savedCalculatedField.setName(UPDATED_CF_NAME);
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);
        doPost("/api/edge/sync/" + edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());
        downlinkMsgs = edgeImitator.getDownlinkMsgs();
        latestMessage = downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg instanceof CalculatedFieldUpdateMsg).findFirst().get();
        Assert.assertTrue(latestMessage instanceof CalculatedFieldUpdateMsg);
        calculatedFieldUpdateMsg = (CalculatedFieldUpdateMsg) latestMessage;
        calculatedFieldFromMsg = JacksonUtil.fromString(calculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
        Assert.assertNotNull(calculatedFieldFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, calculatedFieldUpdateMsg.getMsgType());
        Assert.assertEquals(UPDATED_CF_NAME, calculatedFieldFromMsg.getName());

        // delete calculatedField
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/calculatedField/" + savedCalculatedField.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CalculatedFieldUpdateMsg);
        calculatedFieldUpdateMsg = (CalculatedFieldUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, calculatedFieldUpdateMsg.getMsgType());
        Assert.assertEquals(savedCalculatedField.getUuidId().getMostSignificantBits(), calculatedFieldUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCalculatedField.getUuidId().getLeastSignificantBits(), calculatedFieldUpdateMsg.getIdLSB());
    }

    @Test
    @Ignore
    public void testSendCalculatedFieldToCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);
        UUID uuid = Uuids.timeBased();
        UplinkMsg uplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        checkCalculatedFieldOnCloud(uplinkMsg, uuid, calculatedField.getName());
    }

    @Test
    @Ignore
    public void testUpdateCalculatedFieldNameOnCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);
        UUID uuid = Uuids.timeBased();
        UplinkMsg uplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        checkCalculatedFieldOnCloud(uplinkMsg, uuid, calculatedField.getName());

        calculatedField.setName(UPDATED_CF_NAME);
        UplinkMsg updatedUplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        checkCalculatedFieldOnCloud(updatedUplinkMsg, uuid, calculatedField.getName());
    }

    @Test
    @Ignore
    public void testCalculatedFieldToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);

        edgeImitator.expectMessageAmount(SYNC_MESSAGE_COUNT + 4);
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);
        doPost("/api/edge/sync/" + edge.getId());
        Assert.assertTrue(edgeImitator.waitForMessages());

        UUID uuid = Uuids.timeBased();

        UplinkMsg uplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsg);

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<CalculatedFieldUpdateMsg> calculatedFieldUpdateMsgOpt = edgeImitator.findMessageByType(CalculatedFieldUpdateMsg.class);
        Assert.assertTrue(calculatedFieldUpdateMsgOpt.isPresent());
        CalculatedFieldUpdateMsg latestCalculatedFieldUpdateMsg = calculatedFieldUpdateMsgOpt.get();
        CalculatedField calculatedFieldFromMsg = JacksonUtil.fromString(latestCalculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
        Assert.assertNotNull(calculatedFieldFromMsg);
        Assert.assertNotEquals(DEFAULT_CF_NAME, calculatedFieldFromMsg.getName());

        Assert.assertNotEquals(savedCalculatedField.getUuidId(), uuid);

        CalculatedField calculatedFieldFromCloud = doGet("/api/calculatedField/" + uuid, CalculatedField.class);
        Assert.assertNotNull(calculatedFieldFromCloud);
        Assert.assertNotEquals(DEFAULT_CF_NAME, calculatedFieldFromCloud.getName());
    }

    private CalculatedField createSimpleCalculatedField(EntityId entityId, SimpleCalculatedFieldConfiguration config) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setTenantId(tenantId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName(DEFAULT_CF_NAME);
        calculatedField.setDebugSettings(DebugSettings.all());

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12"); // not used because real telemetry value in db is present
        config.setArguments(Map.of("T", argument));

        config.setExpression("(T * 9/5) + 32");

        Output output = new Output();
        output.setName("fahrenheitTemp");
        output.setType(OutputType.TIME_SERIES);
        output.setDecimalsByDefault(2);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return calculatedField;
    }

    private UplinkMsg getUplinkMsg(UUID uuid, CalculatedField calculatedField, UpdateMsgType updateMsgType) throws InvalidProtocolBufferException {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        CalculatedFieldUpdateMsg.Builder calculatedFieldUpdateMsgBuilder = CalculatedFieldUpdateMsg.newBuilder();
        calculatedFieldUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        calculatedFieldUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        calculatedFieldUpdateMsgBuilder.setEntity(JacksonUtil.toString(calculatedField));
        calculatedFieldUpdateMsgBuilder.setMsgType(updateMsgType);
        testAutoGeneratedCodeByProtobuf(calculatedFieldUpdateMsgBuilder);
        uplinkMsgBuilder.addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        return uplinkMsgBuilder.build();
    }

    private void checkCalculatedFieldOnCloud(UplinkMsg uplinkMsg, UUID uuid, String resourceTitle) throws Exception {
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        CalculatedField calculatedField = doGet("/api/calculatedField/" + uuid, CalculatedField.class);
        Assert.assertNotNull(calculatedField);
        Assert.assertEquals(resourceTitle, calculatedField.getName());
    }

}
