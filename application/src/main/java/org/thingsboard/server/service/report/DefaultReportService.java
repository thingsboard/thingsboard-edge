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
import org.thingsboard.server.common.data.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRequestCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientResponseException;
import org.thingsboard.rule.engine.api.ReportService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
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
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@SuppressWarnings("deprecation")
public class DefaultReportService implements ReportService {

    private static ObjectMapper mapper = new ObjectMapper();
    private static final Pattern reportNameDatePattern = Pattern.compile("%d\\{([^\\}]*)\\}");
    private static final ConcurrentMap<TenantId, TbRateLimits> rateLimits = new ConcurrentHashMap<>();

    @Value("${reports.server.endpointUrl}")
    private String reportsServerEndpointUrl;

    @Value("${reports.rate_limits.enabled:false}")
    private boolean rateLimitsEnabled;

    @Value("${reports.rate_limits.configuration:5:300}")
    private String rateLimitsConfiguration;

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

    private void checkLimits(TenantId tenantId) {
        if (rateLimitsEnabled) {
            TbRateLimits limits = rateLimits.computeIfAbsent(tenantId, t -> new TbRateLimits(rateLimitsConfiguration));
            if (!limits.tryConsume()) {
                log.trace("[{}] Report generation limits exceeded!", tenantId);
                throw new RuntimeException("Failed to generate report due to rate limits!");
            }
        }
    }

    @Override
    public void generateDashboardReport(String baseUrl, DashboardId dashboardId, TenantId tenantId, UserId userId, String publicId,
                                        String reportName, JsonNode reportParams, Consumer<ReportData> onSuccess,
                                        Consumer<Throwable> onFailure) {
        checkLimits(tenantId);
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
        checkLimits(tenantId);
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
                if (t instanceof RestClientResponseException) {
                    onFailure.accept(new ThingsboardException(((RestClientResponseException)t).getStatusText(), ThingsboardErrorCode.GENERAL));
                } else {
                    onFailure.accept(t);
                }
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
