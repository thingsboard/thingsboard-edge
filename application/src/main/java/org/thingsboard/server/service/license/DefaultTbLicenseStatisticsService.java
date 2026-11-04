// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.TbLicenseStatisticsService;
import org.thingsboard.license.shared.TbInstanceStatistics;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.kv.AggregationParams;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.mobile.QrCodeSettingsDao;
import org.thingsboard.server.dao.report.ReportDao;
import org.thingsboard.server.dao.report.ReportTemplateDao;
import org.thingsboard.server.dao.rule.RuleNodeDao;
import org.thingsboard.server.dao.secret.SecretDao;
import org.thingsboard.server.dao.sql.job.JpaJobDao;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateDao;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.ProjectInfo;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;

@Slf4j
@Service
@TbCoreComponent
@ConditionalOnProperty(prefix = "license.stats", value = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DefaultTbLicenseStatisticsService implements TbLicenseStatisticsService {

    private static final List<EntityType> COUNTED_TYPES = List.of(EntityType.TENANT, EntityType.CUSTOMER, EntityType.USER, EntityType.DEVICE,
            EntityType.ASSET, EntityType.RULE_CHAIN, EntityType.DASHBOARD, EntityType.CALCULATED_FIELD, EntityType.DOMAIN, EntityType.QUEUE, EntityType.MOBILE_APP);

    private final EntityDaoRegistry entityDaoRegistry;
    private final IntegrationDao integrationDao;
    private final RuleNodeDao ruleNodeDao;
    private final ConverterDao converterDao;
    private final DashboardDao dashboardDao;
    private final ApiUsageStateDao apiUsageStateDao;
    private final TimeseriesService timeseriesService;
    private final TenantDao tenantDao;
    private final JpaJobDao jpaJobDao;
    private final SecretDao secretDao;
    private final QrCodeSettingsDao qrCodeSettingsDao;
    private final DeviceDao deviceDao;
    private final ProjectInfo projectInfo;
    private final JdbcTemplate jdbcTemplate;
    private final PartitionService partitionService;
    private final ReportDao reportDao;
    private final ReportTemplateDao reportTemplateDao;

    @Value("#{('${database.ts.type}' == 'cassandra') or ('${database.ts_latest.type}' == 'cassandra')}")
    private boolean cassandra;

    @Value("#{('${database.ts.type}' == 'timescale') or ('${database.ts_latest.type}' == 'timescale')}")
    private boolean timescale;

    @Override
    public TbInstanceStatistics getCurrentStatistics() {
        if (partitionService.isSystemPartitionMine(ServiceType.TB_CORE)) {
            return getTbInstanceStatistics();
        }

        return null;
    }

    private TbInstanceStatistics getTbInstanceStatistics() {
        TbInstanceStatistics statistics = new TbInstanceStatistics();

        Map<String, Long> entitiesCounts = new HashMap<>();
        for (EntityType entityType : COUNTED_TYPES) {
            entitiesCounts.put(entityType.name(), getSafely(() -> entityDaoRegistry.getDao(entityType).count()));
        }
        statistics.setEntitiesCounts(entitiesCounts);

        statistics.setRuleNodeTypes(getSafely(() ->
                        ruleNodeDao.countRuleNodesPerType().entrySet().stream()
                                .collect(Collectors.toMap(
                                        entry -> prepareRuleNodeType(entry.getKey()),
                                        Map.Entry::getValue
                                ))
                , Collections.emptyMap()));

        statistics.setIntegrationsCountsPerType(getSafely(integrationDao::countIntegrationsPerType, null));
        statistics.setDevicesCountsPerTransportType(getSafely(deviceDao::countDevicesPerTransportType, null));
        statistics.setJobsByTypeAndStatusLastMonth(getSafely(jpaJobDao::countJobsByTypeAndStatusLastMonth, null));
        statistics.setSecretsPerType(getSafely(secretDao::countSecretsPerType, null));
        statistics.setReportsCountsPerFormatType(getSafely(reportDao::countReportsByType, null));
        statistics.setReportTemplatesByFormatAndType(getSafely(reportTemplateDao::countTemplateByFormatAndType, null));

        statistics.setGenericConverters(getSafely(converterDao::countGenericConverters));
        statistics.setTypedConverters(getSafely(converterDao::countTypedConverters));
        statistics.setDedicatedConverters(getSafely(converterDao::countDedicatedConverters));
        statistics.setJsConvertersCount(getSafely(converterDao::countByJsScriptLang));
        statistics.setTbelConvertersCount(getSafely(converterDao::countByTbelScriptLang));
        statistics.setScadaDashboardsCount(getSafely(dashboardDao::countScadaDashboards));
        statistics.setPostgresDbSize(getSafely((this::getDatabaseSize), -1.0));

        statistics.setQrCodeUsage(getSafely(qrCodeSettingsDao::count));

        statistics.setTbVersion(projectInfo.getProjectVersion());
        statistics.setCassandra(cassandra);
        statistics.setTimescale(timescale);

        statistics.setPlatform(getSafely((() -> System.getProperty("platform", "deb")), "deb"));

        statistics.setSolutionTemplatesCountPerName(getSafely(this::collectSolutionTemplateInstallCounts, Collections.emptyMap()));

        statistics.setApiUsageInLastDay(getSafely((() -> {
            ApiUsageState apiUsage = apiUsageStateDao.findTenantApiUsageState(SYS_TENANT_ID.getId());
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            ZonedDateTime endDay = now.truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime startDay = endDay.minusDays(1);
            long startTs = startDay.toInstant().toEpochMilli();
            long endTs = endDay.toInstant().toEpochMilli();
            List<ReadTsKvQuery> queries = Arrays.stream(ApiUsageRecordKey.values()).map(ApiUsageRecordKey::getApiCountKey).filter(Objects::nonNull).map(key -> new BaseReadTsKvQuery(key + "Hourly", startTs, endTs, AggregationParams.none(), 24, "DESC")).collect(Collectors.toList());

            try {
                return timeseriesService.findAllByQueries(SYS_TENANT_ID, apiUsage.getId(), queries)
                        .get(30, TimeUnit.SECONDS)
                        .stream()
                        .map(ReadTsKvQueryResult::getData)
                        .filter(e -> !e.isEmpty())
                        .flatMap(Collection::stream)
                        .collect(Collectors.groupingBy(KvEntry::getKey, Collectors.toMap(TsKvEntry::getTs, e -> e.getLongValue().orElse(-1L))));
            } catch (Exception e) {
                log.debug("api usage: unable to execute task", e);
                return Collections.emptyMap();
            }
        }), Collections.emptyMap()));

        return statistics;
    }

    private Long getSafely(Supplier<Long> task) {
        return getSafely(task, -1L);
    }

    private <T> T getSafely(Supplier<T> task, T defaultValue) {
        try {
            return task.get();
        } catch (Exception e) {
            log.debug("getSafely: unable to execute task", e);
            return defaultValue;
        }
    }

    private String prepareRuleNodeType(String type) {
        if (!type.startsWith("org.thingsboard")) {
            return "custom." + type.substring(type.lastIndexOf('.') + 1);
        }
        return type;
    }

    private double getDatabaseSize() {
        String sql = "SELECT ROUND(pg_database_size(current_database())::numeric/POWER(1024::numeric,3),2)";
        try {
            return jdbcTemplate.queryForObject(sql, Double.class);
        } catch (Exception e) {
            log.debug("getDatabaseSize(): unable to execute task", e);
            return -1.0;
        }
    }

    private Map<String, Long> collectSolutionTemplateInstallCounts() {
        String sql = "SELECT item_name, COUNT(*) FROM iot_hub_installed_item WHERE item_type = 'SOLUTION_TEMPLATE' GROUP BY item_name";
        Map<String, Long> result = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            result.merge(rs.getString(1), rs.getLong(2), Long::sum);
        });
        return result;
    }

}
