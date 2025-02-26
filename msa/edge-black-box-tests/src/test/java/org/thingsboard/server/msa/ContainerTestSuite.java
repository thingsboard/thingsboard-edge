/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

import static org.thingsboard.server.msa.AbstractContainerTest.TAG_KAFKA;
import static org.thingsboard.server.msa.AbstractContainerTest.TB_EDGE_SERVICE_NAME;
import static org.thingsboard.server.msa.AbstractContainerTest.TB_MONOLITH_SERVICE_NAME;
import static org.thingsboard.server.msa.AbstractContainerTest.edgeConfigurations;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({"org.thingsboard.server.msa.edge.*Test"})
@Slf4j
public class ContainerTestSuite {

    private static final String SOURCE_DIR = "./../../docker-edge/";

    public static DockerComposeContainer<?> testContainer;

    @ClassRule
    public static ThingsBoardDbInstaller installTb = new ThingsBoardDbInstaller();

    @ClassRule
    public static DockerComposeContainer getTestContainer() {
        HashMap<String, String> env = new HashMap<>();
        env.put("CLOUD_RPC_HOST", TB_MONOLITH_SERVICE_NAME);
        for (TestEdgeConfiguration config : edgeConfigurations) {
            env.put("CLOUD_ROUTING_KEY" + config.getTagWithUnderscore().toUpperCase(), config.getRoutingKey());
            env.put("CLOUD_ROUTING_SECRET" + config.getTagWithUnderscore().toUpperCase(), config.getSecret());
        }

        if (testContainer == null) {
            try {
                final String targetDir = FileUtils.getTempDirectoryPath() + "/" + "ContainerTestSuite-" + UUID.randomUUID() + "/";
                log.info("targetDir {}", targetDir);
                FileUtils.copyDirectory(new File(SOURCE_DIR), new File(targetDir));

                final String httpIntegrationDir = "src/test/resources";
                FileUtils.copyDirectory(new File(httpIntegrationDir), new File(targetDir));

                class DockerComposeContainerImpl<SELF extends DockerComposeContainer<SELF>> extends DockerComposeContainer<SELF> {
                    public DockerComposeContainerImpl(File... composeFiles) {
                        super(composeFiles);
                    }

                    @Override
                    public void stop() {
                        super.stop();
                        tryDeleteDir(targetDir);
                    }

                }

                testContainer = new DockerComposeContainerImpl<>(
                        new File("./../../docker-edge/docker-compose.yml"),
                        new File("./../../docker-edge/docker-compose.postgres.yml"),
                        new File("./../../docker-edge/docker-compose.volumes.yml"))
                        .withPull(false)
                        .withLocalCompose(true)
                        .withOptions("--compatibility")
                        .withTailChildContainers(true)
                        .withEnv(installTb.getEnv())
                        .withEnv(env)
                        .withExposedService(TB_MONOLITH_SERVICE_NAME, 8080, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(3)))
                        .withExposedService("kafka", 9092, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(3)));
                for (TestEdgeConfiguration edgeConfiguration : edgeConfigurations) {
                    testContainer.withExposedService(TB_EDGE_SERVICE_NAME + edgeConfiguration.getTagWithDash(), edgeConfiguration.getPort(), Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(3)));
                    if (edgeConfiguration.getTag().equals(TAG_KAFKA)) {
                        testContainer.withExposedService("kafka-edge", 9092, Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(3)));
                    }
                }

            } catch (Exception e) {
                log.error("Failed to create test container", e);
                Assert.fail("Failed to create test container");
            }
        }
        return testContainer;
    }

    private static void tryDeleteDir(String targetDir) {
        try {
            log.info("Trying to delete temp dir {}", targetDir);
            FileUtils.deleteDirectory(new File(targetDir));
        } catch (IOException e) {
            log.error("Can't delete temp directory " + targetDir, e);
        }
    }

}
