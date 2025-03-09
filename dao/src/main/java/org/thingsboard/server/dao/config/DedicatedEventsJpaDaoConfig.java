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
package org.thingsboard.server.dao.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;
import org.thingsboard.server.dao.model.sql.ConverterDebugEventEntity;
import org.thingsboard.server.dao.model.sql.CalculatedFieldDebugEventEntity;
import org.thingsboard.server.dao.model.sql.ErrorEventEntity;
import org.thingsboard.server.dao.model.sql.IntegrationDebugEventEntity;
import org.thingsboard.server.dao.model.sql.LifecycleEventEntity;
import org.thingsboard.server.dao.model.sql.RawDataEventEntity;
import org.thingsboard.server.dao.model.sql.RuleChainDebugEventEntity;
import org.thingsboard.server.dao.model.sql.RuleNodeDebugEventEntity;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import javax.sql.DataSource;
import java.util.Objects;

@DedicatedEventsDataSource
@Configuration
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sql.event", "org.thingsboard.server.dao.sql.audit"},
        bootstrapMode = BootstrapMode.LAZY,
        entityManagerFactoryRef = "eventsEntityManagerFactory", transactionManagerRef = "eventsTransactionManager")
public class DedicatedEventsJpaDaoConfig {

    public static final String EVENTS_PERSISTENCE_UNIT = "events";
    public static final String EVENTS_DATA_SOURCE = EVENTS_PERSISTENCE_UNIT + "DataSource";
    public static final String EVENTS_TRANSACTION_MANAGER = EVENTS_PERSISTENCE_UNIT + "TransactionManager";
    public static final String EVENTS_TRANSACTION_TEMPLATE = EVENTS_PERSISTENCE_UNIT + "TransactionTemplate";
    public static final String EVENTS_JDBC_TEMPLATE = EVENTS_PERSISTENCE_UNIT + "JdbcTemplate";

    @Bean
    @ConfigurationProperties("spring.datasource.events")
    public DataSourceProperties eventsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @ConfigurationProperties(prefix = "spring.datasource.events.hikari")
    @Bean(EVENTS_DATA_SOURCE)
    public DataSource eventsDataSource(@Qualifier("eventsDataSourceProperties") DataSourceProperties eventsDataSourceProperties) {
        return eventsDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean eventsEntityManagerFactory(@Qualifier(EVENTS_DATA_SOURCE) DataSource eventsDataSource,
                                                                             EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(eventsDataSource)
                .packages(LifecycleEventEntity.class, StatisticsEventEntity.class, ErrorEventEntity.class, RuleNodeDebugEventEntity.class, RuleChainDebugEventEntity.class,
                        ConverterDebugEventEntity.class, IntegrationDebugEventEntity.class, RawDataEventEntity.class, AuditLogEntity.class, CalculatedFieldDebugEventEntity.class)
                .persistenceUnit(EVENTS_PERSISTENCE_UNIT)
                .build();
    }

    @Bean(EVENTS_TRANSACTION_MANAGER)
    public JpaTransactionManager eventsTransactionManager(@Qualifier("eventsEntityManagerFactory") LocalContainerEntityManagerFactoryBean eventsEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(eventsEntityManagerFactory.getObject()));
    }

    @Bean(EVENTS_TRANSACTION_TEMPLATE)
    public TransactionTemplate eventsTransactionTemplate(@Qualifier(EVENTS_TRANSACTION_MANAGER) JpaTransactionManager eventsTransactionManager) {
        return new TransactionTemplate(eventsTransactionManager);
    }

    @Bean(EVENTS_JDBC_TEMPLATE)
    public JdbcTemplate eventsJdbcTemplate(@Qualifier(EVENTS_DATA_SOURCE) DataSource eventsDataSource) {
        return new JdbcTemplate(eventsDataSource);
    }

}
