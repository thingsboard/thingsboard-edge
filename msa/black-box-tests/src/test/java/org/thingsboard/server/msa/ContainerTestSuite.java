/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({"org.thingsboard.server.msa.connectivity.EdgeClientTest"})
@Slf4j
public class ContainerTestSuite {

    public static DockerComposeContainer<?> testContainer;

    private static final String SOURCE_DIR = "./../../docker/";

    @ClassRule
    public static ThingsBoardDbInstaller installTb = new ThingsBoardDbInstaller();

//    @ClassRule
//    public static TbEdgeInstaller installEdge = new TbEdgeInstaller();

    @ClassRule
    public static DockerComposeContainer getTestContainer() {
        HashMap<String, String> env = new HashMap<>();
        env.put("EDGE_DOCKER_REPO", "thingsboard");
        env.put("TB_EDGE_DOCKER_NAME", "tb-edge");
        env.put("TB_EDGE_VERSION", "3.3.2EDGE-SNAPSHOT");
        env.put("CLOUD_ROUTING_KEY", "280629c7-f853-ee3d-01c0-fffbb6f2ef38");
        env.put("CLOUD_ROUTING_SECRET", "g9ta4soeylw6smqkky8g");
        env.put("CLOUD_RPC_HOST", "tb-monolith");

        if (testContainer == null) {
            boolean skipTailChildContainers = Boolean.valueOf(System.getProperty("blackBoxTests.skipTailChildContainers"));
            try {
                final String targetDir = FileUtils.getTempDirectoryPath() + "/" + "ContainerTestSuite-" + UUID.randomUUID() + "/";
                log.info("targetDir {}", targetDir);
                FileUtils.copyDirectory(new File(SOURCE_DIR), new File(targetDir));
                replaceInFile(targetDir + "advanced/docker-compose.yml", "    container_name: \"${LOAD_BALANCER_NAME}\"", "", "container_name");

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
                        new File("./../../docker/docker-compose.yml"),
                        new File("./../../docker/docker-compose.postgres.yml"),
                        new File("./../../docker/docker-compose.postgres.volumes.yml"))
                        .withPull(false)
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

    /**
     * This workaround is actual until issue will be resolved:
     * Support container_name in docker-compose file #2472 https://github.com/testcontainers/testcontainers-java/issues/2472
     * docker-compose files which contain container_name are not supported and the creation of DockerComposeContainer fails due to IllegalStateException.
     * This has been introduced in #1151 as a quick fix for unintuitive feedback. https://github.com/testcontainers/testcontainers-java/issues/1151
     * Using the latest testcontainers and waiting for the fix...
     * */
    private static void replaceInFile(String sourceFilename, String target, String replacement, String verifyPhrase) {
        try {
            File file = new File(sourceFilename);
            String sourceContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            String outputContent = sourceContent.replace(target, replacement);
            assertThat(outputContent, (not(containsString(target))));
            assertThat(outputContent, (not(containsString(verifyPhrase))));

            FileUtils.writeStringToFile(file, outputContent, StandardCharsets.UTF_8);
            assertThat(FileUtils.readFileToString(file, StandardCharsets.UTF_8), is(outputContent));
        } catch (IOException e) {
            log.error("failed to update file " + sourceFilename, e);
            fail("failed to update file");
        }
    }
}
