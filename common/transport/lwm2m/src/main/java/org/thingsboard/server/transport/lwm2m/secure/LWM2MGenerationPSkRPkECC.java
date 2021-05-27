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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

@Slf4j
public class LWM2MGenerationPSkRPkECC {

    public LWM2MGenerationPSkRPkECC() {
        generationPSkKey();
        generationRPKECCKey();
    }

    private void generationPSkKey() {
        /* PSK */
        int lenPSkKey = 32;
        /* Start PSK
          Clients and Servers MUST support PSK keys of up to 64 bytes in length, as required by [RFC7925]
          SecureRandom object must be unpredictable, and all SecureRandom output sequences must be cryptographically strong, as described in [RFC4086]
          */
        SecureRandom randomPSK = new SecureRandom();
        byte[] bytesPSK = new byte[lenPSkKey];
        randomPSK.nextBytes(bytesPSK);
        log.info("\nCreating new PSK: \n for the next start PSK -> security key: [{}]", Hex.encodeHexString(bytesPSK));
    }

    private void generationRPKECCKey() {
        /* RPK */
        String algorithm = "EC";
        String provider = "SunEC";
        String nameParameterSpec = "secp256r1";

        /* Start RPK
          Elliptic Curve parameters  : [secp256r1 [NIST P-256, X9.62 prime256v1] (1.2.840.10045.3.1.7)]
          */
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance(algorithm, provider);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            log.error("", e);
        }
        ECGenParameterSpec ecsp = new ECGenParameterSpec(nameParameterSpec);
        try {
            kpg.initialize(ecsp);
        } catch (InvalidAlgorithmParameterException e) {
            log.error("", e);
        }

        KeyPair kp = kpg.genKeyPair();
        PrivateKey privKey = kp.getPrivate();
        PublicKey pubKey = kp.getPublic();

        if (pubKey instanceof ECPublicKey) {
            ECPublicKey ecPublicKey = (ECPublicKey) pubKey;
            /* Get x coordinate */
            byte[] x = ecPublicKey.getW().getAffineX().toByteArray();
            if (x[0] == 0)
                x = Arrays.copyOfRange(x, 1, x.length);

            /* Get Y coordinate */
            byte[] y = ecPublicKey.getW().getAffineY().toByteArray();
            if (y[0] == 0)
                y = Arrays.copyOfRange(y, 1, y.length);

            /* Get Curves params */
            String privHex = Hex.encodeHexString(privKey.getEncoded());
            log.info("\nCreating new RPK for the next start... \n" +
                            " Public Key (Hex): [{}]\n" +
                            " Private Key (Hex): [{}]" +
                            " public_x :  [{}] \n" +
                            " public_y :  [{}] \n" +
                            " private_encode : [{}] \n" +
                            " Elliptic Curve parameters  : [{}] \n",
                    Hex.encodeHexString(pubKey.getEncoded()),
                    privHex,
                    Hex.encodeHexString(x),
                    Hex.encodeHexString(y),
                    privHex,
                    ecPublicKey.getParams().toString());
        }
    }
}

