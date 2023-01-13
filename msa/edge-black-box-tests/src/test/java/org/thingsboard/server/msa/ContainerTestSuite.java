/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({"org.thingsboard.server.msa.edge.*Test"})
@Slf4j
public class ContainerTestSuite {

    public static DockerComposeContainer<?> testContainer;

    private static final String SOURCE_DIR = "./../../docker-edge/";

    @ClassRule
    public static ThingsBoardDbInstaller installTb = new ThingsBoardDbInstaller();

    @ClassRule
    public static DockerComposeContainer getTestContainer() {
        HashMap<String, String> env = new HashMap<>();
        env.put("EDGE_DOCKER_REPO", "thingsboard");
        env.put("TB_EDGE_DOCKER_NAME", "tb-edge-pe");
        env.put("TB_EDGE_VERSION", "3.5.0EDGEPE-SNAPSHOT");
        env.put("CLOUD_ROUTING_KEY", "280629c7-f853-ee3d-01c0-fffbb6f2ef38");
        env.put("CLOUD_ROUTING_SECRET", "g9ta4soeylw6smqkky8g");
        env.put("CLOUD_RPC_HOST", "tb-monolith");

        if (testContainer == null) {
            boolean skipTailChildContainers = Boolean.valueOf(System.getProperty("edgeBlackBoxTests.skipTailChildContainers"));
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
                        new File("./../../docker-edge/docker-compose.postgres.volumes.yml"))
                        .withPull(false)
                        .withOptions("--compatibility")
                        .withLocalCompose(true)
                        .withTailChildContainers(!skipTailChildContainers)
                        .withEnv(installTb.getEnv())
                        .withEnv(env)
                        .withEnv("LOAD_BALANCER_NAME", "")
                        .withExposedService("tb-edge", 8082)
                        .withExposedService("haproxy", 80, Wait.forHttp("/swagger-ui.html").withStartupTimeout(Duration.ofSeconds(60)));
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
