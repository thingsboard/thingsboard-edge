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
package org.thingsboard.server.dao;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;

import javax.script.ScriptException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public abstract class AbstractNoSqlContainer {

    public static final List<String> INIT_SCRIPTS = List.of(
            "cassandra/schema-keyspace.cql",
            "cassandra/schema-ts.cql",
            "cassandra/schema-ts-latest.cql"
    );

    @ClassRule(order = 0)
    public static final CassandraContainer cassandra = (CassandraContainer) new CassandraContainer("cassandra:4.1") {
        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            super.containerIsStarted(containerInfo);
            DatabaseDelegate db = new CassandraDatabaseDelegate(this);
            INIT_SCRIPTS.forEach(script -> runInitScriptIfRequired(db, script));
        }

        private void runInitScriptIfRequired(DatabaseDelegate db, String initScriptPath) {
            logger().info("Init script [{}]", initScriptPath);
            if (initScriptPath != null) {
                try {
                    URL resource = Thread.currentThread().getContextClassLoader().getResource(initScriptPath);
                    if (resource == null) {
                        logger().warn("Could not load classpath init script: {}", initScriptPath);
                        throw new ScriptUtils.ScriptLoadException("Could not load classpath init script: " + initScriptPath + ". Resource not found.");
                    }
                    String cql = IOUtils.toString(resource, StandardCharsets.UTF_8);
                    ScriptUtils.executeDatabaseScript(db, initScriptPath, cql);
                } catch (IOException e) {
                    logger().warn("Could not load classpath init script: {}", initScriptPath);
                    throw new ScriptUtils.ScriptLoadException("Could not load classpath init script: " + initScriptPath, e);
                } catch (ScriptException e) {
                    logger().error("Error while executing init script: {}", initScriptPath, e);
                    throw new ScriptUtils.UncategorizedScriptException("Error while executing init script: " + initScriptPath, e);
                }
            }
        }
    }
            .withEnv("HEAP_NEWSIZE", "64M")
            .withEnv("MAX_HEAP_SIZE", "512M")
            .withEnv("CASSANDRA_CLUSTER_NAME", "ThingsBoard Cluster");

    @ClassRule(order = 1)
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            cassandra.start();
            String cassandraUrl = String.format("%s:%s", cassandra.getHost(), cassandra.getMappedPort(9042));
            log.debug("Cassandra url [{}]", cassandraUrl);
            System.setProperty("cassandra.url", cassandraUrl);
        }

        @Override
        protected void after() {
            cassandra.stop();
            List.of("cassandra.url")
                    .forEach(System.getProperties()::remove);
        }
    };

}
