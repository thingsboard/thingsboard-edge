/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRequestCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResponseExtractor;
import org.thingsboard.rule.engine.api.ReportService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DefaultReportService implements ReportService {

    private static ObjectMapper mapper = new ObjectMapper();
    private static final Pattern reportNameDatePattern = Pattern.compile("%d\\{([^\\}]*)\\}");

    @Value("${reports.server.endpointUrl}")
    private String reportsServerEndpointUrl;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private JwtTokenFactory jwtTokenFactory;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;


    private EventLoopGroup eventLoopGroup;
    private AsyncRestTemplate httpClient;

    @PostConstruct
    public void init() {
        try {
            this.eventLoopGroup = new NioEventLoopGroup();
            Netty4ClientHttpRequestFactory nettyFactory = new Netty4ClientHttpRequestFactory(this.eventLoopGroup);
            nettyFactory.setSslContext(SslContextBuilder.forClient().build());
            httpClient = new AsyncRestTemplate(nettyFactory);
        } catch (SSLException e) {
            log.error("Can't initialize report service due to {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void generateDashboardReport(String baseUrl, DashboardId dashboardId, TenantId tenantId, UserId userId, String publicId,
                                        String reportName, JsonNode reportParams, Consumer<ReportData> onSuccess,
                                        Consumer<Throwable> onFailure) {
        log.trace("Executing generateDashboardReport, baseUrl [{}], dashboardId [{}], userId [{}]", baseUrl, dashboardId, userId);

        AccessJwtToken accessToken;
        if (StringUtils.isEmpty(publicId)) {
            accessToken = calculateUserAccessToken(tenantId, userId);
        } else {
            accessToken = calculateUserAccessTokenFromPublicId(tenantId, publicId);
            ((ObjectNode) reportParams).put("publicId", publicId);
        }

        String token = accessToken.getToken();
        long expiration = accessToken.getClaims().getExpiration().getTime();

        ObjectNode dashboardReportRequest = mapper.createObjectNode();
        dashboardReportRequest.put("baseUrl", baseUrl);
        dashboardReportRequest.put("dashboardId", dashboardId.toString());
        dashboardReportRequest.set("reportParams", reportParams);
        dashboardReportRequest.put("name", reportName);
        dashboardReportRequest.put("token", token);
        dashboardReportRequest.put("expiration", expiration);

        requestReport(dashboardReportRequest, null, onSuccess, onFailure);
    }

    @Override
    public void generateReport(TenantId tenantId, ReportConfig reportConfig, String reportsServerEndpointUrl, Consumer<ReportData> onSuccess, Consumer<Throwable> onFailure) {
        log.trace("Executing generateReport, reportConfig [{}]", reportConfig);

        JsonNode dashboardReportRequest = createDashboardReportRequest(tenantId, reportConfig);
        requestReport(dashboardReportRequest, reportsServerEndpointUrl, onSuccess, onFailure);
    }

    private void requestReport(JsonNode dashboardReportRequest, String reportsServerEndpointUrl, Consumer<ReportData> onSuccess,
                               Consumer<Throwable> onFailure) {
        if (StringUtils.isEmpty(reportsServerEndpointUrl)) {
            reportsServerEndpointUrl = this.reportsServerEndpointUrl;
        }
        String endpointUrl = reportsServerEndpointUrl + "/dashboardReport";

        org.springframework.util.concurrent.ListenableFuture<ReportData> reportDataFuture = httpClient.execute(endpointUrl, HttpMethod.POST,
                new ReportRequestCallback(dashboardReportRequest), responseExtractor);
        reportDataFuture.addCallback(new ListenableFutureCallback<ReportData>() {
            @Override
            public void onSuccess(ReportData result) {
                try {
                    onSuccess.accept(result);
                } catch (Throwable th) {
                    onFailure(th);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                onFailure.accept(t);
            }
        });
    }

    private JsonNode createDashboardReportRequest(TenantId tenantId, ReportConfig reportConfig) {
        AccessJwtToken accessToken = calculateUserAccessToken(tenantId, new UserId(UUID.fromString(reportConfig.getUserId())));
        String token = accessToken.getToken();
        long expiration = accessToken.getClaims().getExpiration().getTime();
        TimeZone tz = TimeZone.getTimeZone(reportConfig.getTimezone());
        String reportName = prepareReportName(reportConfig.getNamePattern(), new Date(), tz);
        ObjectNode dashboardReportRequest = mapper.createObjectNode();
        dashboardReportRequest.put("baseUrl", reportConfig.getBaseUrl());
        dashboardReportRequest.put("dashboardId", reportConfig.getDashboardId());
        dashboardReportRequest.put("token", token);
        dashboardReportRequest.put("expiration", expiration);
        dashboardReportRequest.put("name", reportName);
        dashboardReportRequest.set("reportParams", createReportParams(reportConfig));
        return dashboardReportRequest;
    }

    private JsonNode createReportParams(ReportConfig reportConfig) {
        ObjectNode reportParams = mapper.createObjectNode();
        reportParams.put("type", reportConfig.getType());
        reportParams.put("state", reportConfig.getState());
        if (!reportConfig.isUseDashboardTimewindow()) {
            reportParams.set("timewindow", reportConfig.getTimewindow());
        }
        reportParams.put("timezone", reportConfig.getTimezone());
        return reportParams;
    }

    private String prepareReportName(String namePattern, Date reportDate, TimeZone tz) {
        String name = namePattern;
        Matcher matcher = reportNameDatePattern.matcher(namePattern);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            dateFormat.setTimeZone(tz);
            String replacement = dateFormat.format(reportDate);
            name = name.replace(toReplace, replacement);
        }
        return name;
    }

    private AccessJwtToken calculateUserAccessToken(TenantId tenantId, UserId userId) {
        User user = userService.findUserById(tenantId, userId);
        UserCredentials credentials = userService.findUserCredentialsByUserId(tenantId, userId);
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        MergedUserPermissions mergedUserPermissions;
        try {
            mergedUserPermissions = userPermissionsService.getMergedPermissions(user, false);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }

        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal, mergedUserPermissions);
        return jwtTokenFactory.createAccessJwtToken(securityUser);
    }

    private AccessJwtToken calculateUserAccessTokenFromPublicId(TenantId tenantId, String publicId) {
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        Customer publicCustomer = customerService.findCustomerById(tenantId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found: " + publicId);
        }
        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        SecurityUser securityUser = new SecurityUser(user, true, new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId), new MergedUserPermissions(new HashMap<>(), new HashMap<>()));
        return jwtTokenFactory.createAccessJwtToken(securityUser);
    }

    final ResponseExtractor<ReportData> responseExtractor = response -> {
        ReportData reportData = new ReportData();
        reportData.setData(IOUtils.toByteArray(response.getBody()));
        reportData.setContentType(response.getHeaders().getContentType().toString());
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String fileName = disposition.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
        fileName = URLDecoder.decode(fileName, "ISO_8859_1");
        reportData.setName(fileName);
        return reportData;
    };

    final class ReportRequestCallback implements AsyncRequestCallback {

        private JsonNode body;

        public ReportRequestCallback(JsonNode body) {
            this.body = body;
        }

        @Override
        public void doWithRequest(AsyncClientHttpRequest request) throws IOException {
            request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] json = getRequestBytes();
            request.getHeaders().setContentLength(json.length);
            request.getHeaders().setConnection("keep-alive");
            request.getBody().write(json);
        }

        byte[] getRequestBytes() throws JsonProcessingException {
            return mapper.writeValueAsBytes(body);
        }

    }

}
