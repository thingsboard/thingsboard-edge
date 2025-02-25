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
package org.thingsboard.server.dao.audit.sink;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.TenantId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(prefix = "audit-log.sink", value = "type", havingValue = "elasticsearch")
@Slf4j
public class ElasticsearchAuditLogSink implements AuditLogSink {

    private static final String TENANT_PLACEHOLDER = "@{TENANT}";
    private static final String DATE_PLACEHOLDER = "@{DATE}";
    private static final String INDEX_TYPE = "audit_log";

    @Value("${audit-log.sink.index_pattern}")
    private String indexPattern;
    @Value("${audit-log.sink.scheme_name}")
    private String schemeName;
    @Value("${audit-log.sink.host}")
    private String host;
    @Value("${audit-log.sink.port}")
    private int port;
    @Value("${audit-log.sink.user_name}")
    private String userName;
    @Value("${audit-log.sink.password}")
    private String password;
    @Value("${audit-log.sink.date_format}")
    private String dateFormat;

    private RestClient restClient;
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        try {
            log.trace("Adding elastic rest endpoint... host [{}], port [{}], scheme name [{}]",
                    host, port, schemeName);
            RestClientBuilder builder = RestClient.builder(
                    new HttpHost(host, port, schemeName));

            if (StringUtils.isNotEmpty(userName) &&
                    StringUtils.isNotEmpty(password)) {
                log.trace("...using username [{}] and password ***", userName);
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(userName, password));
                builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }

            this.restClient = builder.build();
            this.executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("elasticsearch-audit-log"));
        } catch (Exception e) {
            log.error("Sink init failed!", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreDestroy
    private void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void logAction(AuditLog auditLogEntry) {
        executor.execute(() -> {
            try {
                doLogAction(auditLogEntry);
            } catch (Exception e) {
                log.error("Failed to log action", e);
            }
        });
    }

    private void doLogAction(AuditLog auditLogEntry) {
        String jsonContent = createElasticJsonRecord(auditLogEntry);

        HttpEntity entity = new NStringEntity(
                jsonContent,
                ContentType.APPLICATION_JSON);

        Request request = new Request(HttpMethod.POST.name(),String.format("/%s/%s", getIndexName(auditLogEntry.getTenantId()), INDEX_TYPE));
        request.setEntity(entity);

        restClient.performRequestAsync(request, responseListener);
    }

    private String createElasticJsonRecord(AuditLog auditLog) {
        ObjectNode auditLogNode = JacksonUtil.newObjectNode();
        auditLogNode.put("postDate", LocalDateTime.now().toString());
        auditLogNode.put("id", auditLog.getId().getId().toString());
        auditLogNode.put("entityName", auditLog.getEntityName());
        auditLogNode.put("tenantId", auditLog.getTenantId().getId().toString());
        if (auditLog.getCustomerId() != null) {
            auditLogNode.put("customerId", auditLog.getCustomerId().getId().toString());
        }
        auditLogNode.put("entityId", auditLog.getEntityId().getId().toString());
        auditLogNode.put("entityType", auditLog.getEntityId().getEntityType().name());
        auditLogNode.put("userId", auditLog.getUserId().getId().toString());
        auditLogNode.put("userName", auditLog.getUserName());
        auditLogNode.put("actionType", auditLog.getActionType().name());
        if (auditLog.getActionData() != null) {
            auditLogNode.put("actionData", auditLog.getActionData().toString());
        }
        auditLogNode.put("actionStatus", auditLog.getActionStatus().name());
        auditLogNode.put("actionFailureDetails", auditLog.getActionFailureDetails());
        return auditLogNode.toString();
    }

    private ResponseListener responseListener = new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
            log.trace("Elasticsearch sink log action method succeeded. Response result [{}]!", response);
        }

        @Override
        public void onFailure(Exception exception) {
            log.warn("Elasticsearch sink log action method failed!", exception);
        }
    };

    private String getIndexName(TenantId tenantId) {
        String indexName = indexPattern;
        if (indexName.contains(TENANT_PLACEHOLDER) && tenantId != null) {
            indexName = indexName.replace(TENANT_PLACEHOLDER, tenantId.getId().toString());
        }
        if (indexName.contains(DATE_PLACEHOLDER)) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            indexName = indexName.replace(DATE_PLACEHOLDER, now.format(formatter));
        }
        return indexName.toLowerCase();
    }
}
