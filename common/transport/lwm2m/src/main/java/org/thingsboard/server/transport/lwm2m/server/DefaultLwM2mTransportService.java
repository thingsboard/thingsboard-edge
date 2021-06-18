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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.LWM2MGenerationPSkRPkECC;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.DefaultLwM2MUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mNetworkConfig.getCoapConfig;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FIRMWARE_UPDATE_COAP_RECOURSE;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mTransportService implements LwM2MTransportService {

    public static final CipherSuite[] RPK_OR_X509_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256};
    public static final CipherSuite[] PSK_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256};
    private PublicKey publicKey;
    private PrivateKey privateKey;

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    private final LwM2mTransportServerHelper helper;
    private final OtaPackageDataCache otaPackageDataCache;
    private final DefaultLwM2MUplinkMsgHandler handler;
    private final CaliforniumRegistrationStore registrationStore;
    private final TbSecurityStore securityStore;
    private final LwM2mClientContext lwM2mClientContext;
    private final TbLwM2MDtlsCertificateVerifier certificateVerifier;
    private final TbLwM2MAuthorizer authorizer;

    private LeshanServer server;

    @PostConstruct
    public void init() {
        if (config.getEnableGenNewKeyPskRpk()) {
            new LWM2MGenerationPSkRPkECC();
        }
        this.server = getLhServer();
        /**
         * Add a resource to the server.
         * CoapResource ->
         * path = FW_PACKAGE or SW_PACKAGE
         * nameFile = "BC68JAR01A09_TO_BC68JAR01A10.bin"
         * "coap://host:port/{path}/{token}/{nameFile}"
         */

        LwM2mTransportCoapResource otaCoapResource = new LwM2mTransportCoapResource(otaPackageDataCache, FIRMWARE_UPDATE_COAP_RECOURSE);
        this.server.coap().getServer().add(otaCoapResource);
        this.startLhServer();
        this.context.setServer(server);
    }

    private void startLhServer() {
        log.info("Starting LwM2M transport server...");
        this.server.start();
        LwM2mServerListener lhServerCertListener = new LwM2mServerListener(handler);
        this.server.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.server.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.server.getObservationService().addListener(lhServerCertListener.observationListener);
        log.info("Started LwM2M transport server.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport server!");
        server.destroy();
        log.info("LwM2M transport server stopped!");
    }

    private LeshanServer getLhServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(config.getHost(), config.getPort());
        builder.setLocalSecureAddress(config.getSecureHost(), config.getSecurePort());
        builder.setDecoder(new DefaultLwM2mNodeDecoder());
        /* Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mNodeEncoder(LwM2mValueConverterImpl.getInstance()));

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        /* Define model provider (Create Models )*/
        LwM2mModelProvider modelProvider = new LwM2mVersionedModelProvider(this.lwM2mClientContext, this.helper, this.context);
        config.setModelProvider(modelProvider);
        builder.setObjectModelProvider(modelProvider);

        /* Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
        builder.setRegistrationStore(registrationStore);


        /* Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();

        dtlsConfig.setServerOnly(true);
        dtlsConfig.setRecommendedSupportedGroupsOnly(config.isRecommendedSupportedGroups());
        dtlsConfig.setRecommendedCipherSuitesOnly(config.isRecommendedCiphers());
        /*  Create credentials */
        this.setServerWithCredentials(builder, dtlsConfig);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /* Create LWM2M server */
        return builder.build();
    }

    private void setServerWithCredentials(LeshanServerBuilder builder, DtlsConnectorConfig.Builder dtlsConfig) {
        if (config.getKeyStoreValue() != null && this.setBuilderX509(builder)) {
            dtlsConfig.setAdvancedCertificateVerifier(certificateVerifier);
            builder.setAuthorizer(authorizer);
            dtlsConfig.setSupportedCipherSuites(RPK_OR_X509_CIPHER_SUITES);
        } else if (this.setServerRPK(builder)) {
            this.infoPramsUri("RPK");
            this.infoParamsServerKey(this.publicKey, this.privateKey);
            dtlsConfig.setSupportedCipherSuites(RPK_OR_X509_CIPHER_SUITES);
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
            log.info("Unable to load X509 files for LWM2MServer");
            dtlsConfig.setSupportedCipherSuites(PSK_CIPHER_SUITES);
            this.infoPramsUri("PSK");
        }
    }

    private boolean setBuilderX509(LeshanServerBuilder builder) {
        try {
            X509Certificate serverCertificate = (X509Certificate) config.getKeyStoreValue().getCertificate(config.getCertificateAlias());
            PrivateKey privateKey = (PrivateKey) config.getKeyStoreValue().getKey(config.getCertificateAlias(), config.getKeyStorePassword() == null ? null : config.getKeyStorePassword().toCharArray());
            PublicKey publicKey = serverCertificate.getPublicKey();
            if (privateKey != null && privateKey.getEncoded().length > 0 && publicKey != null && publicKey.getEncoded().length > 0) {
                builder.setPublicKey(serverCertificate.getPublicKey());
                builder.setPrivateKey(privateKey);
                builder.setCertificateChain(new X509Certificate[]{serverCertificate});
                this.infoParamsServerX509(serverCertificate, publicKey, privateKey);
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
            return false;
        }
    }

    private void infoParamsServerX509(X509Certificate certificate, PublicKey publicKey, PrivateKey privateKey) {
        try {
            infoPramsUri("X509");
            log.info("\n- X509 Certificate (Hex): [{}]",
                    Hex.encodeHexString(certificate.getEncoded()));
            this.infoParamsServerKey(publicKey, privateKey);
        } catch (CertificateEncodingException e) {
            log.error("", e);
        }
    }

    private void infoPramsUri(String mode) {
        LwM2MTransportServerConfig lwM2MTransportServerConfig = config;
        log.info("Server uses [{}]: serverNoSecureURI : [{}:{}], serverSecureURI : [{}:{}]", mode,
                lwM2MTransportServerConfig.getHost(),
                lwM2MTransportServerConfig.getPort(),
                lwM2MTransportServerConfig.getSecureHost(),
                lwM2MTransportServerConfig.getSecurePort());
    }

    private boolean setServerRPK(LeshanServerBuilder builder) {
        try {
            this.loadOrGenerateRPKKeys();
            if (this.publicKey != null && this.publicKey.getEncoded().length > 0 &&
                    this.privateKey != null && this.privateKey.getEncoded().length > 0) {
                builder.setPublicKey(this.publicKey);
                builder.setPrivateKey(this.privateKey);
                return true;
            }
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException e) {
            log.error("Fail create Server with RPK", e);
        }
        return false;
    }

    private void loadOrGenerateRPKKeys() throws NoSuchAlgorithmException, InvalidParameterSpecException, InvalidKeySpecException {
        /* Get Elliptic Curve Parameter spec for secp256r1 */
        AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
        algoParameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
        LwM2MTransportServerConfig serverConfig = config;
        if (StringUtils.isNotEmpty(serverConfig.getPublicX()) && StringUtils.isNotEmpty(serverConfig.getPublicY())) {
            byte[] publicX = Hex.decodeHex(serverConfig.getPublicX().toCharArray());
            byte[] publicY = Hex.decodeHex(serverConfig.getPublicY().toCharArray());
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            this.publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
        }
        String privateEncodedKey = serverConfig.getPrivateEncoded();
        if (StringUtils.isNotEmpty(privateEncodedKey)) {
            byte[] privateS = Hex.decodeHex(privateEncodedKey.toCharArray());
            try {
                this.privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(privateS));
            } catch (InvalidKeySpecException ignore2) {
                log.error("Invalid Server rpk.PrivateKey.getEncoded () [{}}]. PrivateKey has no EC algorithm", privateEncodedKey);
            }
        }
    }

    private void infoParamsServerKey(PublicKey publicKey, PrivateKey privateKey) {
        /* Get x coordinate */
        byte[] x = ((ECPublicKey) publicKey).getW().getAffineX().toByteArray();
        if (x[0] == 0)
            x = Arrays.copyOfRange(x, 1, x.length);

        /* Get Y coordinate */
        byte[] y = ((ECPublicKey) publicKey).getW().getAffineY().toByteArray();
        if (y[0] == 0)
            y = Arrays.copyOfRange(y, 1, y.length);

        /* Get Curves params */
        String params = ((ECPublicKey) publicKey).getParams().toString();
        String privHex = Hex.encodeHexString(privateKey.getEncoded());
        log.info(" \n- Public Key (Hex): [{}] \n" +
                        "- Private Key (Hex): [{}], \n" +
                        "public_x: \"${LWM2M_SERVER_PUBLIC_X:{}}\" \n" +
                        "public_y: \"${LWM2M_SERVER_PUBLIC_Y:{}}\" \n" +
                        "private_encoded: \"${LWM2M_SERVER_PRIVATE_ENCODED:{}}\" \n" +
                        "- Elliptic Curve parameters  : [{}]",
                Hex.encodeHexString(publicKey.getEncoded()),
                privHex,
                Hex.encodeHexString(x),
                Hex.encodeHexString(y),
                privHex,
                params);
    }

    @Override
    public String getName() {
        return "LWM2M";
    }

}
