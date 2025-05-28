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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.sql.audit.AuditLogRepository;
import org.thingsboard.server.dao.sql.event.EventRepository;
import org.thingsboard.server.dao.util.TbAutoConfiguration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.thingsboard.server.dao.sql", "org.thingsboard.server.dao.attributes", "org.thingsboard.server.dao.sqlts.dictionary", "org.thingsboard.server.dao.cache", "org.thingsboard.server.cache", "org.thingsboard.server.dao.entity"})
@EnableJpaRepositories(value = {"org.thingsboard.server.dao.sql", "org.thingsboard.server.dao.sqlts.dictionary"},
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {EventRepository.class, AuditLogRepository.class}),
        bootstrapMode = BootstrapMode.LAZY)
public class JpaDaoConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    @Bean
    public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("dataSource") DataSource dataSource,
                                                                       EntityManagerFactoryBuilder builder,
                                                                       @Autowired(required = false) SqlTsLatestDaoConfig tsLatestDaoConfig,
                                                                       @Autowired(required = false) SqlTsDaoConfig tsDaoConfig,
                                                                       @Autowired(required = false) TimescaleDaoConfig timescaleDaoConfig,
                                                                       @Autowired(required = false) TimescaleTsLatestDaoConfig timescaleTsLatestDaoConfig) {
        List<String> packages = new ArrayList<>();
        packages.add("org.thingsboard.server.dao.model.sql");
        packages.add("org.thingsboard.server.dao.model.sqlts.dictionary");
        if (tsLatestDaoConfig != null) {
            packages.add("org.thingsboard.server.dao.model.sqlts.latest");
        }
        if (tsDaoConfig != null) {
            packages.add("org.thingsboard.server.dao.model.sqlts.ts");
        }
        if (timescaleDaoConfig != null) {
            packages.add("org.thingsboard.server.dao.model.sqlts.timescale");
        }
        if (timescaleTsLatestDaoConfig != null) {
            packages.add("org.thingsboard.server.dao.model.sqlts.latest");
        }
        return builder
                .dataSource(dataSource)
                .packages(packages.toArray(String[]::new))
                .persistenceUnit("default")
                .build();
    }

    @Primary
    @Bean
    public JpaTransactionManager transactionManager(@Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }

    @Primary
    @Bean
    public TransactionTemplate transactionTemplate(@Qualifier("transactionManager") JpaTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Primary
    @Bean
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Primary
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

}
