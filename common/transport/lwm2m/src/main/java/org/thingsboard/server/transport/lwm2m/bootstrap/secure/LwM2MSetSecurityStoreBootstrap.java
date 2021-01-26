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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.LwM2MTransportContextBootstrap;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.server.LwM2MTransportContextServer;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.ECPoint;
import java.security.spec.KeySpec;
import java.security.spec.ECPrivateKeySpec;
import java.util.Arrays;

import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.X509;

@Slf4j
@Data
public class LwM2MSetSecurityStoreBootstrap {

    private KeyStore keyStore;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private LwM2MTransportContextBootstrap contextBs;
    private LwM2MTransportContextServer contextS;
    private LeshanBootstrapServerBuilder builder;
    EditableSecurityStore securityStore;

    public LwM2MSetSecurityStoreBootstrap(LeshanBootstrapServerBuilder builder, LwM2MTransportContextBootstrap contextBs, LwM2MTransportContextServer contextS, LwM2MSecurityMode dtlsMode) {
        this.builder = builder;
        this.contextBs = contextBs;
        this.contextS = contextS;
        /** Set securityStore with new registrationStore */

        switch (dtlsMode) {
            /** Use No_Sec only */
            case NO_SEC:
                setServerWithX509Cert(NO_SEC.code);
                break;
            /** Use PSK/RPK  */
            case PSK:
            case RPK:
                setRPK();
                break;
            case X509:
                setServerWithX509Cert(X509.code);
                break;
            /** Use X509_EST only */
            case X509_EST:
                // TODO support sentinel pool and make pool configurable
                break;
            /** Use ather X509, PSK,  No_Sec ?? */
            default:
                break;
        }
    }

    private void setRPK() {
        try {
            /** Get Elliptic Curve Parameter spec for secp256r1 */
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
            if (this.contextBs.getCtxBootStrap().getBootstrapPublicX() != null && !this.contextBs.getCtxBootStrap().getBootstrapPublicX().isEmpty() && this.contextBs.getCtxBootStrap().getBootstrapPublicY() != null && !this.contextBs.getCtxBootStrap().getBootstrapPublicY().isEmpty()) {
                /** Get point values */
                byte[] publicX = Hex.decodeHex(this.contextBs.getCtxBootStrap().getBootstrapPublicX().toCharArray());
                byte[] publicY = Hex.decodeHex(this.contextBs.getCtxBootStrap().getBootstrapPublicY().toCharArray());
                /** Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                /** Get keys */
                this.publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            }
            if (this.contextBs.getCtxBootStrap().getBootstrapPrivateS() != null && !this.contextBs.getCtxBootStrap().getBootstrapPrivateS().isEmpty()) {
                /** Get point values */
                byte[] privateS = Hex.decodeHex(this.contextBs.getCtxBootStrap().getBootstrapPrivateS().toCharArray());
                /** Create key specs */
                KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);
                /** Get keys */
                this.privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);
            }
            if (this.publicKey != null && this.publicKey.getEncoded().length > 0 &&
                    this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                this.builder.setPublicKey(this.publicKey);
                this.builder.setPrivateKey(this.privateKey);
                this.contextBs.getCtxBootStrap().setBootstrapPublicKey(this.publicKey);
                getParamsRPK();
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("[{}] Failed generate Server PSK/RPK", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void setServerWithX509Cert(int securityModeCode) {
        try {
            if (this.contextS.getCtxServer().getKeyStoreValue() != null) {
                KeyStore keyStoreServer = this.contextS.getCtxServer().getKeyStoreValue();
                setBuilderX509();
                X509Certificate rootCAX509Cert = (X509Certificate) keyStoreServer.getCertificate(this.contextS.getCtxServer().getRootAlias());
                if (rootCAX509Cert != null && securityModeCode == X509.code) {
                    X509Certificate[] trustedCertificates = new X509Certificate[1];
                    trustedCertificates[0] = rootCAX509Cert;
                    this.builder.setTrustedCertificates(trustedCertificates);
                } else {
                    /** by default trust all */
                    this.builder.setTrustedCertificates(new X509Certificate[0]);
                }
            }
            else {
                /** by default trust all */
                this.builder.setTrustedCertificates(new X509Certificate[0]);
                log.error("Unable to load X509 files for BootStrapServer");
            }
        } catch (KeyStoreException ex) {
            log.error("[{}] Unable to load X509 files server", ex.getMessage());
        }

    }

    private void setBuilderX509() {
        /**
         * For deb => KeyStorePathFile == yml or commandline: KEY_STORE_PATH_FILE
         * For idea => KeyStorePathResource == common/transport/lwm2m/src/main/resources/credentials: in LwM2MTransportContextServer: credentials/serverKeyStore.jks
         */
        try {
            X509Certificate serverCertificate = (X509Certificate) this.contextS.getCtxServer().getKeyStoreValue().getCertificate(this.contextBs.getCtxBootStrap().getBootstrapAlias());
            this.privateKey = (PrivateKey) this.contextS.getCtxServer().getKeyStoreValue().getKey(this.contextBs.getCtxBootStrap().getBootstrapAlias(), this.contextS.getCtxServer().getKeyStorePasswordServer() == null ? null : this.contextS.getCtxServer().getKeyStorePasswordServer().toCharArray());
            if (this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                this.builder.setPrivateKey(this.privateKey);
            }
            if (serverCertificate != null) {
                this.builder.setCertificateChain(new X509Certificate[]{serverCertificate});
                this.contextBs.getCtxBootStrap().setBootstrapCertificate(serverCertificate);
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
        }
    }

    private void getParamsRPK() {
        if (this.publicKey instanceof ECPublicKey) {
            /** Get x coordinate */
            byte[] x = ((ECPublicKey) this.publicKey).getW().getAffineX().toByteArray();
            if (x[0] == 0)
                x = Arrays.copyOfRange(x, 1, x.length);

            /** Get Y coordinate */
            byte[] y = ((ECPublicKey) this.publicKey).getW().getAffineY().toByteArray();
            if (y[0] == 0)
                y = Arrays.copyOfRange(y, 1, y.length);

            /** Get Curves params */
            String params = ((ECPublicKey) this.publicKey).getParams().toString();
            log.info(
                    " \nBootstrap uses RPK : \n Elliptic Curve parameters  : [{}] \n Public x coord : [{}] \n Public y coord : [{}] \n Public Key (Hex): [{}] \n Private Key (Hex): [{}]",
                    params, Hex.encodeHexString(x), Hex.encodeHexString(y),
                    Hex.encodeHexString(this.publicKey.getEncoded()),
                    Hex.encodeHexString(this.privateKey.getEncoded()));
        } else {
            throw new IllegalStateException("Unsupported Public Key Format (only ECPublicKey supported).");
        }
    }

//    private void getParamsX509() {
//        try {
//            log.info("BootStrap uses X509 : \n X509 Certificate (Hex): [{}] \n Private Key (Hex): [{}]",
//                    Hex.encodeHexString(this.certificate.getEncoded()),
//                    Hex.encodeHexString(this.privateKey.getEncoded()));
//        } catch (CertificateEncodingException e) {
//            e.printStackTrace();
//        }
//    }
}
