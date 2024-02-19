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

import com.eclipsesource.json.JsonObject;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.leshan.core.request.Identity;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LwM2MIdentitySerDesTest {

    @Test
    void serializePskIdentity() {
        assertThat(LwM2MIdentitySerDes.serialize(Identity.psk(getTestAddress(), "my:psk")).toString())
                .isEqualTo("{\"type\":\"psk\",\"id\":\"my:psk\"}");
    }


    @Test
    void serializeRpkIdentity() {
        var public_key = mock(PublicKey.class);
        when(public_key.getEncoded()).thenReturn(new byte[]{1,2,3,4,5,6,7,8,9});

        assertThat(LwM2MIdentitySerDes.serialize(Identity.rpk(getTestAddress(), public_key)).toString())
                .isEqualTo("{\"type\":\"rpk\",\"rpk\":\"010203040506070809\"}");
    }

    @Test
    void serializeX509Identity() {
        assertThat(LwM2MIdentitySerDes.serialize(Identity.x509(getTestAddress(), "MyCommonName")).toString())
                .isEqualTo("{\"type\":\"x509\",\"cn\":\"MyCommonName\"}");
    }

    @Test
    void serializeUnsecureIdentity() {
        assertThat(LwM2MIdentitySerDes.serialize(Identity.unsecure(getTestAddress())).toString())
                .isEqualTo("{\"type\":\"unsecure\",\"address\":\"1.2.3.4\",\"port\":5684}");
    }
    

    @Test
    void deserialize() {
        assertThatThrownBy(() -> LwM2MIdentitySerDes.deserialize(mock(JsonObject.class)))
                .isInstanceOf(NotImplementedException.class);
    }

    private static InetSocketAddress getTestAddress() {
        try {
            return new InetSocketAddress(InetAddress.getByName("1.2.3.4"), 5684);
        } catch (UnknownHostException e) {
            throw new AssertionError("Cannot create test address");
        }
    }
}