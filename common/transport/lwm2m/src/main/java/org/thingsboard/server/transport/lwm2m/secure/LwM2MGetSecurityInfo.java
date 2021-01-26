/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.secure;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.transport.lwm2m.bootstrap.LwM2MTransportContextBootstrap;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler;
import org.thingsboard.server.transport.lwm2m.utils.TypeServer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.*;

@Slf4j
@Component("LwM2MGetSecurityInfo")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MGetSecurityInfo {

    @Autowired
    public LwM2MTransportContextServer contextS;

    @Autowired
    public LwM2MTransportContextBootstrap contextBS;


    public ReadResultSecurityStore getSecurityInfo(String endPoint, TypeServer keyValue) {
        CountDownLatch latch = new CountDownLatch(1);
        final ReadResultSecurityStore[] resultSecurityStore = new ReadResultSecurityStore[1];
        contextS.getTransportService().process(ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(endPoint).build(),
                new TransportServiceCallback<ValidateDeviceCredentialsResponseMsg>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponseMsg msg) {
                        String credentialsBody = msg.getCredentialsBody();
                        resultSecurityStore[0] = putSecurityInfo(endPoint, msg.getDeviceInfo().getDeviceName(), credentialsBody, keyValue);
                        resultSecurityStore[0].setMsg(msg);
                        Optional<DeviceProfile> deviceProfileOpt = LwM2MTransportHandler.decode(msg.getProfileBody().toByteArray());
                        deviceProfileOpt.ifPresent(profile -> resultSecurityStore[0].setDeviceProfile(profile));
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] Failed to process credentials PSK: {}", endPoint, e);
                        resultSecurityStore[0] = putSecurityInfo(endPoint, null, null, null);
                        latch.countDown();
                    }
                });
        try {
            latch.await(contextS.getCtxServer().getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resultSecurityStore[0];
    }

    private ReadResultSecurityStore putSecurityInfo(String endPoint, String deviceName, String jsonStr, TypeServer keyValue) {
        ReadResultSecurityStore result = new ReadResultSecurityStore();
        JsonObject objectMsg = LwM2MTransportHandler.validateJson(jsonStr);
        if (objectMsg != null && !objectMsg.isJsonNull()) {
            JsonObject object = (objectMsg.has(keyValue.type) && !objectMsg.get(keyValue.type).isJsonNull()) ? objectMsg.get(keyValue.type).getAsJsonObject() : null;
            /**
             * Only PSK
             */
            String endPointPsk = (objectMsg.has("client")
                    && objectMsg.get("client").getAsJsonObject().has("endpoint")
                    && objectMsg.get("client").getAsJsonObject().get("endpoint").isJsonPrimitive()) ? objectMsg.get("client").getAsJsonObject().get("endpoint").getAsString() : null;
            endPoint = (endPointPsk == null || endPointPsk.isEmpty()) ? endPoint : endPointPsk;
            if (object != null && !object.isJsonNull()) {
                if (keyValue.equals(TypeServer.BOOTSTRAP)) {
                    result.setBootstrapJsonCredential(object);
                    result.setEndPoint(endPoint);
                } else {
                    LwM2MSecurityMode lwM2MSecurityMode = LwM2MSecurityMode.fromSecurityMode(object.get("securityConfigClientMode").getAsString().toLowerCase());
                    switch (lwM2MSecurityMode) {
                        case NO_SEC:
                            getClientSecurityInfoNoSec(result);
                            break;
                        case PSK:
                            getClientSecurityInfoPSK(result, endPoint, object);
                            break;
                        case RPK:
                            getClientSecurityInfoRPK(result, endPoint, object);
                            break;
                        case X509:
                            getClientSecurityInfoX509(result, endPoint);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return result;
    }

    private void getClientSecurityInfoNoSec(ReadResultSecurityStore result) {
        result.setSecurityInfo(null);
        result.setSecurityMode(NO_SEC.code);
    }

    private void getClientSecurityInfoPSK(ReadResultSecurityStore result, String endPoint, JsonObject object) {
        /** PSK Deserialization */
        String identity = (object.has("identity") && object.get("identity").isJsonPrimitive()) ? object.get("identity").getAsString() : null;
        if (identity != null && !identity.isEmpty()) {
            try {
                byte[] key = (object.has("key") && object.get("key").isJsonPrimitive()) ? Hex.decodeHex(object.get("key").getAsString().toCharArray()) : null;
                if (key != null && key.length > 0) {
                    if (endPoint != null && !endPoint.isEmpty()) {
                        result.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(endPoint, identity, key));
                        result.setSecurityMode(PSK.code);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Missing PSK key: " + e.getMessage());
            }
        } else {
            log.error("Missing PSK identity");
        }
    }

    private void getClientSecurityInfoRPK(ReadResultSecurityStore result, String endpoint, JsonObject object) {
        try {
            if (object.has("key") && object.get("key").isJsonPrimitive()) {
                byte[] rpkkey = Hex.decodeHex(object.get("key").getAsString().toLowerCase().toCharArray());
                PublicKey key = SecurityUtil.publicKey.decode(rpkkey);
                result.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(endpoint, key));
                result.setSecurityMode(RPK.code);
            } else {
                log.error("Missing RPK key");
            }
        } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
            log.error("RPK: Invalid security info content: " + e.getMessage());
        }
    }

    private void getClientSecurityInfoX509(ReadResultSecurityStore result, String endpoint) {
        result.setSecurityInfo(SecurityInfo.newX509CertInfo(endpoint));
        result.setSecurityMode(X509.code);
    }
}
