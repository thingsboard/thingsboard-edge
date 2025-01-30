/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.msa;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.ExternalResource;
import org.testcontainers.utility.Base58;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.msa.AbstractContainerTest.edgeConfigurations;

@Slf4j
public class ThingsBoardDbInstaller extends ExternalResource {

    private final static String POSTGRES_DATA_VOLUME = "tb-postgres-test-data-volume";
    private final static String TB_LOG_VOLUME = "tb-log-test-volume";
    private final static String TB_EDGE_LOG_VOLUME = "tb-edge-log-test-volume";
    private final static String TB_EDGE_DATA_VOLUME = "tb-edge-data-test-volume";

    private final DockerComposeExecutor dockerCompose;

    private final String postgresDataVolume;
    private final String tbLogVolume;
    private final String tbEdgeLogVolume;
    private final String tbEdgeDataVolume;

    @Getter
    private final Map<String, String> env;

    public ThingsBoardDbInstaller() {
        try {
            List<File> composeFiles = Arrays.asList(new File("./../../docker-edge/docker-compose.yml"),
                    new File("./../../docker-edge/docker-compose.postgres.yml"),
                    new File("./../../docker-edge/docker-compose.volumes.yml"));

            String identifier = Base58.randomString(6).toLowerCase();
            String project = identifier + Base58.randomString(6).toLowerCase();

            postgresDataVolume = project + "_" + POSTGRES_DATA_VOLUME;
            tbLogVolume = project + "_" + TB_LOG_VOLUME;
            tbEdgeLogVolume = project + "_" + TB_EDGE_LOG_VOLUME;
            tbEdgeDataVolume = project + "_" + TB_EDGE_DATA_VOLUME;

            dockerCompose = new DockerComposeExecutor(composeFiles, project);

            Dotenv dotenv = Dotenv.configure().directory("./../../docker-edge").filename(".env").load();

            env = new HashMap<>();
            for (DotenvEntry entry : dotenv.entries()) {
                env.put(entry.getKey(), entry.getValue());
            }
            env.put("POSTGRES_DATA_VOLUME", postgresDataVolume);
            env.put("TB_LOG_VOLUME", tbLogVolume);

            for (TestEdgeConfiguration config : edgeConfigurations) {
                env.put("SPRING_DATASOURCE_URL" + config.getTagWithUnderscore().toUpperCase(), "jdbc:postgresql://postgres:5432/tb_edge" + config.getTagWithUnderscore());
                env.put("TB_EDGE_LOG_VOLUME" + config.getTagWithUnderscore().toUpperCase(), tbEdgeLogVolume + config.getTagWithDash());
                env.put("TB_EDGE_DATA_VOLUME" + config.getTagWithUnderscore().toUpperCase(), tbEdgeDataVolume + config.getTagWithDash());
                env.put("CLOUD_ROUTING_KEY" + config.getTagWithUnderscore().toUpperCase(), config.getRoutingKey());
                env.put("CLOUD_ROUTING_SECRET" + config.getTagWithUnderscore().toUpperCase(), config.getSecret());
            }

            dockerCompose.withEnv(env);
        } catch (Exception e) {
            log.error("Failed to create ThingsBoardDbInstaller", e);
            throw e;
        }
    }

    @Override
    protected void before() throws Throwable {
        try {

            dockerCompose.withCommand("volume create " + postgresDataVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbLogVolume);
            dockerCompose.invokeDocker();
            for (TestEdgeConfiguration config : edgeConfigurations) {
                dockerCompose.withCommand("volume create " + tbEdgeLogVolume + config.getTagWithDash());
                dockerCompose.invokeDocker();

                dockerCompose.withCommand("volume create " + tbEdgeDataVolume  + config.getTagWithDash());
                dockerCompose.invokeDocker();
            }

            dockerCompose.withCommand("up -d postgres");
            dockerCompose.invokeCompose();

            dockerCompose.withCommand("run --no-deps --rm -e INSTALL_TB=true -e LOAD_DEMO=true tb-monolith");
            dockerCompose.invokeCompose();

            for (TestEdgeConfiguration config : edgeConfigurations) {
                dockerCompose.withCommand("run --no-deps --rm -e INSTALL_TB_EDGE=true -e LOAD_DEMO=true tb-edge" + config.getTagWithDash());
                dockerCompose.invokeCompose();
            }
            dockerCompose.withCommand("exec -T postgres psql -U postgres -d thingsboard -f /custom-sql/thingsboard.sql");
            dockerCompose.invokeCompose();
            for (TestEdgeConfiguration config : edgeConfigurations) {
                dockerCompose.withCommand("exec -T postgres psql -U postgres -d tb_edge" + config.getTagWithUnderscore() + " -f /custom-sql/tb_edge.sql");
                dockerCompose.invokeCompose();
            }
        } finally {
            try {
                dockerCompose.withCommand("down -v");
                dockerCompose.invokeCompose();
            } catch (Exception e) {
                log.error("Failed [before]", e);
            }
        }
    }

    @Override
    protected void after() {
        try {
            for (TestEdgeConfiguration config : edgeConfigurations) {
                copyLogs(tbLogVolume, "./target/tb-logs/");
                copyLogs(tbEdgeLogVolume + config.getTagWithDash(), "./target/tb-edge-logs/");

                dockerCompose.withCommand("volume rm -f " + postgresDataVolume + " " + tbLogVolume + " " + tbEdgeLogVolume + config.getTagWithDash());
                dockerCompose.invokeDocker();
            }
        } catch (Exception e) {
            log.error("Failed [after]", e);
            throw e;
        }
    }

    private void copyLogs(String volumeName, String targetDir) {
        try {
            File tbLogsDir = new File(targetDir);
            tbLogsDir.mkdirs();

            String logsContainerName = "tb-logs-container-" + StringUtils.randomAlphanumeric(10);

            dockerCompose.withCommand("run -d --rm --name " + logsContainerName + " -v " + volumeName + ":/root alpine tail -f /dev/null");
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("cp " + logsContainerName + ":/root/. " + tbLogsDir.getAbsolutePath());
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("rm -f " + logsContainerName);
            dockerCompose.invokeDocker();
        } catch (Exception e) {
            log.error("Failed [copy logs]", e);
            throw e;
        }
    }

}
