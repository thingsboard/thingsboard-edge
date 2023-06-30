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
package org.thingsboard.rule.engine.rest;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.credentials.BasicCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.rule.engine.credentials.CredentialsType;
import org.thingsboard.rule.engine.mail.TbMsgToEmailNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Data
@Slf4j
public class TbHttpClient {

    private static final String STATUS = "status";
    private static final String STATUS_CODE = "statusCode";
    private static final String STATUS_REASON = "statusReason";
    private static final String ERROR = "error";
    private static final String ERROR_BODY = "error_body";
    private static final String ERROR_SYSTEM_PROPERTIES = "Didn't set any system proxy properties. Should be added next system proxy properties: \"http.proxyHost\" and \"http.proxyPort\" or  \"https.proxyHost\" and \"https.proxyPort\" or \"socksProxyHost\" and \"socksProxyPort\"";

    private static final String HTTP_PROXY_HOST = "http.proxyHost";
    private static final String HTTP_PROXY_PORT = "http.proxyPort";
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";

    private static final String SOCKS_PROXY_HOST = "socksProxyHost";
    private static final String SOCKS_PROXY_PORT = "socksProxyPort";
    private static final String SOCKS_VERSION = "socksProxyVersion";
    private static final String SOCKS_VERSION_5 = "5";
    private static final String SOCKS_VERSION_4 = "4";
    public static final String PROXY_USER = "tb.proxy.user";
    public static final String PROXY_PASSWORD = "tb.proxy.password";

    private final TbRestApiCallNodeConfiguration config;

    private EventLoopGroup eventLoopGroup;
    private WebClient webClient;
    private Semaphore semaphore;

    TbHttpClient(TbRestApiCallNodeConfiguration config, EventLoopGroup eventLoopGroupShared) throws TbNodeException {
        try {
            this.config = config;
            if (config.getMaxParallelRequestsCount() > 0) {
                semaphore = new Semaphore(config.getMaxParallelRequestsCount());
            }

            HttpClient httpClient = HttpClient.create()
                    .runOn(getSharedOrCreateEventLoopGroup(eventLoopGroupShared))
                    .doOnConnected(c ->
                            c.addHandlerLast(new ReadTimeoutHandler(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)));

            if (config.isEnableProxy()) {
                if (config.isUseSystemProxyProperties()) {
                    checkSystemProxyProperties();
                    httpClient = httpClient.proxy(this::createSystemProxyProvider);
                } else {
                    checkProxyHost(config.getProxyHost());
                    checkProxyPort(config.getProxyPort());
                    String proxyUser = config.getProxyUser();
                    String proxyPassword = config.getProxyPassword();

                    httpClient = httpClient.proxy(options -> {
                        var o = options.type(ProxyProvider.Proxy.HTTP)
                                .host(config.getProxyHost())
                                .port(config.getProxyPort());

                        if (useAuth(proxyUser, proxyPassword)) {
                            o.username(proxyUser).password(u -> proxyPassword);
                        }
                    });
                    SslContext sslContext = SslContextBuilder.forClient().build();
                    httpClient.secure(t -> t.sslContext(sslContext));
                }
            } else if (!config.isUseSimpleClientHttpFactory()) {
                if (CredentialsType.CERT_PEM == config.getCredentials().getType()) {
                    throw new TbNodeException("Simple HTTP Factory does not support CERT PEM credentials!");
                }
            } else {
                SslContext sslContext = config.getCredentials().initSslContext();
                httpClient = httpClient.secure(t -> t.sslContext(sslContext));
            }

            this.webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        } catch (SSLException e) {
            throw new TbNodeException(e);
        }
    }

    EventLoopGroup getSharedOrCreateEventLoopGroup(EventLoopGroup eventLoopGroupShared) {
        if (eventLoopGroupShared != null) {
            return eventLoopGroupShared;
        }
        return this.eventLoopGroup = new NioEventLoopGroup();
    }

    private void checkSystemProxyProperties() throws TbNodeException {
        boolean useHttpProxy = !StringUtils.isEmpty(System.getProperty("http.proxyHost")) && !StringUtils.isEmpty(System.getProperty("http.proxyPort"));
        boolean useHttpsProxy = !StringUtils.isEmpty(System.getProperty("https.proxyHost")) && !StringUtils.isEmpty(System.getProperty("https.proxyPort"));
        boolean useSocksProxy = !StringUtils.isEmpty(System.getProperty("socksProxyHost")) && !StringUtils.isEmpty(System.getProperty("socksProxyPort"));
        if (!(useHttpProxy || useHttpsProxy || useSocksProxy)) {
            log.warn(ERROR_SYSTEM_PROPERTIES);
            throw new TbNodeException(ERROR_SYSTEM_PROPERTIES);
        }
    }

    private boolean useAuth(String proxyUser, String proxyPassword) {
        return !StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(proxyPassword);
    }

    void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    public void processMessage(TbContext ctx, TbMsg msg,
                               Consumer<TbMsg> onSuccess,
                               BiConsumer<TbMsg, Throwable> onFailure) {
        try {
            if (semaphore != null && !semaphore.tryAcquire(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)) {
                ctx.tellFailure(msg, new RuntimeException("Timeout during waiting for reply!"));
                return;
            }

            String endpointUrl = TbNodeUtils.processPattern(config.getRestEndpointUrlPattern(), msg);
            HttpMethod method = HttpMethod.valueOf(config.getRequestMethod());
            URI uri = buildEncodedUri(endpointUrl);

            RequestBodySpec request = webClient
                    .method(method)
                    .uri(uri)
                    .headers(headers -> prepareHeaders(headers, msg));

            if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) ||
                    HttpMethod.PATCH.equals(method) || HttpMethod.DELETE.equals(method) ||
                    config.isIgnoreRequestBody()) {
                request.body(BodyInserters.fromValue(getData(ctx, msg)));
            }

            request
                    .retrieve()
                    .toEntity(String.class)
                    .subscribe(responseEntity -> {
                        if (semaphore != null) {
                            semaphore.release();
                        }

                        if (responseEntity.getStatusCode().is2xxSuccessful()) {
                            onSuccess.accept(processResponse(ctx, msg, responseEntity));
                        } else {
                            onFailure.accept(processFailureResponse(ctx, msg, responseEntity), null);
                        }
                    }, throwable -> {
                        if (semaphore != null) {
                            semaphore.release();
                        }

                        onFailure.accept(processException(ctx, msg, throwable), throwable);
                    });
        } catch (InterruptedException e) {
            log.warn("Timeout during waiting for reply!", e);
        }
    }

    public URI buildEncodedUri(String endpointUrl) {
        if (endpointUrl == null) {
            throw new RuntimeException("Url string cannot be null!");
        }
        if (endpointUrl.isEmpty()) {
            throw new RuntimeException("Url string cannot be empty!");
        }

        URI uri = UriComponentsBuilder.fromUriString(endpointUrl).build().encode().toUri();
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            throw new RuntimeException("Transport scheme(protocol) must be provided!");
        }

        boolean authorityNotValid = uri.getAuthority() == null || uri.getAuthority().isEmpty();
        boolean hostNotValid = uri.getHost() == null || uri.getHost().isEmpty();
        if (authorityNotValid || hostNotValid) {
            throw new RuntimeException("Url string is invalid!");
        }

        return uri;
    }

    private String getData(TbContext ctx, TbMsg msg) {
        String data = msg.getData();

        List<BlobEntityId> attachments = new ArrayList<>();
        String attachmentsStr = msg.getMetaData().getValue(TbMsgToEmailNode.ATTACHMENTS);
        if (!StringUtils.isEmpty(attachmentsStr)) {
            String[] attachmentsStrArray = attachmentsStr.split(",");
            for (String attachmentStr : attachmentsStrArray) {
                attachments.add(new BlobEntityId(UUID.fromString(attachmentStr)));
            }
        }

        if (!attachments.isEmpty()) {
            BlobEntity blobEntity = ctx.getPeContext().getBlobEntityService().findBlobEntityById(ctx.getTenantId(), attachments.get(0));
            if (blobEntity != null) {
                data = StandardCharsets.UTF_8.decode(blobEntity.getData()).toString();
            } else {
                log.warn("[{}] Attachments {} not found", ctx.getTenantId(), attachmentsStr);
            }
        }

        if (config.isTrimDoubleQuotes()) {
            final String dataBefore = data;
            data = data.replaceAll("^\"|\"$", "");
            log.trace("Trimming double quotes. Before trim: [{}], after trim: [{}]", dataBefore, data);
        }

        return data;
    }

    private TbMsg processResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        HttpStatus httpStatus = (HttpStatus) response.getStatusCode();
        metaData.putValue(STATUS, httpStatus.name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, httpStatus.getReasonPhrase());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        String body = response.getBody() == null ? "{}" : response.getBody();
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, body);
    }

    void headersToMetaData(Map<String, List<String>> headers, BiConsumer<String, String> consumer) {
        if (headers == null) {
            return;
        }
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                if (values.size() == 1) {
                    consumer.accept(key, values.get(0));
                } else {
                    consumer.accept(key, JacksonUtil.toString(values));
                }
            }
        });
    }

    private TbMsg processFailureResponse(TbContext ctx, TbMsg origMsg, ResponseEntity<String> response) {
        HttpStatus httpStatus = (HttpStatus) response.getStatusCode();
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, httpStatus.name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, httpStatus.getReasonPhrase());
        metaData.putValue(ERROR_BODY, response.getBody());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        if (e instanceof RestClientResponseException) {
            RestClientResponseException restClientResponseException = (RestClientResponseException) e;
            metaData.putValue(STATUS, restClientResponseException.getStatusText());
            metaData.putValue(STATUS_CODE, restClientResponseException.getRawStatusCode() + "");
            metaData.putValue(ERROR_BODY, restClientResponseException.getResponseBodyAsString());
        }
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private void prepareHeaders(HttpHeaders headers, TbMsg msg) {
        config.getHeaders().forEach((k, v) -> headers.add(TbNodeUtils.processPattern(k, msg), TbNodeUtils.processPattern(v, msg)));
        ClientCredentials credentials = config.getCredentials();
        if (CredentialsType.BASIC == credentials.getType()) {
            BasicCredentials basicCredentials = (BasicCredentials) credentials;
            String authString = basicCredentials.getUsername() + ":" + basicCredentials.getPassword();
            String encodedAuthString = new String(Base64.encodeBase64(authString.getBytes(StandardCharsets.UTF_8)));
            headers.add("Authorization", "Basic " + encodedAuthString);
        }
    }

    private static void checkProxyHost(String proxyHost) {
        if (StringUtils.isEmpty(proxyHost)) {
            throw new IllegalArgumentException("Proxy host can't be empty");
        }
    }

    private static void checkProxyPort(int proxyPort) {
        if (proxyPort < 0 || proxyPort > 65535) {
            throw new IllegalArgumentException("Proxy port out of range:" + proxyPort);
        }
    }

    private void createSystemProxyProvider(ProxyProvider.TypeSpec option) {
        Properties properties = System.getProperties();
        if (properties.containsKey(HTTP_PROXY_HOST) || properties.containsKey(HTTPS_PROXY_HOST)) {
            createHttpProxyFrom(option, properties);
        }
        if (properties.containsKey(SOCKS_PROXY_HOST)) {
            createSocksProxyFrom(option, properties);
        }
    }

    private void createHttpProxyFrom(ProxyProvider.TypeSpec option, Properties properties) {
        String hostProperty;
        String portProperty;
        if (properties.containsKey(HTTPS_PROXY_HOST)) {
            hostProperty = HTTPS_PROXY_HOST;
            portProperty = HTTPS_PROXY_PORT;
        } else {
            hostProperty = HTTP_PROXY_HOST;
            portProperty = HTTP_PROXY_PORT;
        }

        String hostname = properties.getProperty(hostProperty);
        int port = Integer.parseInt(properties.getProperty(portProperty));

        checkProxyHost(config.getProxyHost());
        checkProxyPort(config.getProxyPort());

        var proxy = option
                .type(ProxyProvider.Proxy.HTTP)
                .host(hostname)
                .port(port);

        var proxyUser = properties.getProperty(PROXY_USER);
        var proxyPassword = properties.getProperty(PROXY_PASSWORD);

        if (useAuth(proxyUser, proxyPassword)) {
            proxy.username(proxyUser).password(u -> proxyPassword);
        }
    }

    private void createSocksProxyFrom(ProxyProvider.TypeSpec option, Properties properties) {
        String hostname = properties.getProperty(SOCKS_PROXY_HOST);
        String version = properties.getProperty(SOCKS_VERSION, SOCKS_VERSION_5);
        if (!SOCKS_VERSION_5.equals(version) && !SOCKS_VERSION_4.equals(version)) {
            throw new IllegalArgumentException(String.format("Wrong socks version %s! Supported only socks versions 4 and 5.", version));
        }

        ProxyProvider.Proxy type = SOCKS_VERSION_5.equals(version) ? ProxyProvider.Proxy.SOCKS5 : ProxyProvider.Proxy.SOCKS4;
        int port = Integer.parseInt(properties.getProperty(SOCKS_PROXY_PORT));

        checkProxyHost(config.getProxyHost());
        checkProxyPort(config.getProxyPort());

        ProxyProvider.Builder proxy = option
                .type(type)
                .host(hostname)
                .port(port);

        var proxyUser = properties.getProperty(PROXY_USER);
        var proxyPassword = properties.getProperty(PROXY_PASSWORD);

        if (useAuth(proxyUser, proxyPassword)) {
            proxy.username(proxyUser).password(u -> proxyPassword);
        }
    }

}
