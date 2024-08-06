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
import org.thingsboard.server.dao.model.sql.ErrorEventEntity;
import org.thingsboard.server.dao.model.sql.LifecycleEventEntity;
import org.thingsboard.server.dao.model.sql.RuleChainDebugEventEntity;
import org.thingsboard.server.dao.model.sql.RuleNodeDebugEventEntity;
import org.thingsboard.server.dao.model.sql.StatisticsEventEntity;

import javax.sql.DataSource;
import java.util.Objects;

/*
 * To make entity use a dedicated datasource:
 * - add its JpaRepository to exclusions list in @EnableJpaRepositories in JpaDaoConfig
 * - add the package of this JpaRepository to @EnableJpaRepositories in DefaultDedicatedJpaDaoConfig
 * - add the package of this JpaRepository to @EnableJpaRepositories in DedicatedJpaDaoConfig
 * - add the entity class to packages list in dedicatedEntityManagerFactory in DedicatedJpaDaoConfig
 * */
@DedicatedDataSource
@Configuration
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sql.event", "org.thingsboard.server.dao.sql.audit"},
        bootstrapMode = BootstrapMode.LAZY,
        entityManagerFactoryRef = "dedicatedEntityManagerFactory", transactionManagerRef = "dedicatedTransactionManager")
public class DedicatedJpaDaoConfig {

    public static final String DEDICATED_PERSISTENCE_UNIT = "dedicated";
    public static final String DEDICATED_TRANSACTION_MANAGER = DEDICATED_PERSISTENCE_UNIT + "TransactionManager";
    public static final String DEDICATED_TRANSACTION_TEMPLATE = DEDICATED_PERSISTENCE_UNIT + "TransactionTemplate";
    public static final String DEDICATED_JDBC_TEMPLATE = DEDICATED_PERSISTENCE_UNIT + "JdbcTemplate";

    @Bean
    @ConfigurationProperties("spring.datasource.dedicated")
    public DataSourceProperties dedicatedDataSourceProperties() {
        return new DataSourceProperties();
    }

    @ConfigurationProperties(prefix = "spring.datasource.dedicated.hikari")
    @Bean
    public DataSource dedicatedDataSource(@Qualifier("dedicatedDataSourceProperties") DataSourceProperties dedicatedDataSourceProperties) {
        return dedicatedDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean dedicatedEntityManagerFactory(@Qualifier("dedicatedDataSource") DataSource dedicatedDataSource,
                                                                                EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dedicatedDataSource)
                .packages(LifecycleEventEntity.class, StatisticsEventEntity.class, ErrorEventEntity.class, RuleNodeDebugEventEntity.class, RuleChainDebugEventEntity.class, AuditLogEntity.class)
                .persistenceUnit(DEDICATED_PERSISTENCE_UNIT)
                .build();
    }

    @Bean(DEDICATED_TRANSACTION_MANAGER)
    public JpaTransactionManager dedicatedTransactionManager(@Qualifier("dedicatedEntityManagerFactory") LocalContainerEntityManagerFactoryBean dedicatedEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(dedicatedEntityManagerFactory.getObject()));
    }

    @Bean(DEDICATED_TRANSACTION_TEMPLATE)
    public TransactionTemplate dedicatedTransactionTemplate(@Qualifier(DEDICATED_TRANSACTION_MANAGER) JpaTransactionManager dedicatedTransactionManager) {
        return new TransactionTemplate(dedicatedTransactionManager);
    }

    @Bean(DEDICATED_JDBC_TEMPLATE)
    public JdbcTemplate dedicatedJdbcTemplate(@Qualifier("dedicatedDataSource") DataSource dedicatedDataSource) {
        return new JdbcTemplate(dedicatedDataSource);
    }

}
