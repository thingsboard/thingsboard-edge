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
package org.thingsboard.server.transport.lwm2m.server.store.util;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Hex;

import java.security.PublicKey;

public class LwM2MIdentitySerDes {

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PORT = "port";
    private static final String KEY_ID = "id";
    private static final String KEY_CN = "cn";
    private static final String KEY_RPK = "rpk";
    protected static final String KEY_LWM2MIDENTITY_TYPE = "type";
    protected static final String LWM2MIDENTITY_TYPE_UNSECURE = "unsecure";
    protected static final String LWM2MIDENTITY_TYPE_PSK = "psk";
    protected static final String LWM2MIDENTITY_TYPE_X509 = "x509";
    protected static final String LWM2MIDENTITY_TYPE_RPK = "rpk";

    public static JsonObject serialize(Identity identity) {
        JsonObject o = Json.object();

        if (identity.isPSK()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_PSK);
            o.set(KEY_ID, identity.getPskIdentity());
        } else if (identity.isRPK()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_RPK);
            PublicKey publicKey = identity.getRawPublicKey();
            o.set(KEY_RPK, Hex.encodeHexString(publicKey.getEncoded()));
        } else if (identity.isX509()) {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_X509);
            o.set(KEY_CN, identity.getX509CommonName());
        } else {
            o.set(KEY_LWM2MIDENTITY_TYPE, LWM2MIDENTITY_TYPE_UNSECURE);
            o.set(KEY_ADDRESS, identity.getPeerAddress().getHostString());
            o.set(KEY_PORT, identity.getPeerAddress().getPort());
        }
        return o;
    }

    public static Identity deserialize(JsonObject peer) {
        throw new NotImplementedException();
    }
}