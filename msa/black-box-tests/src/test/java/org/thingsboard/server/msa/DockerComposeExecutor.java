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
package org.thingsboard.server.msa;


import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.CommandLine;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

@Slf4j
public class DockerComposeExecutor {

    String ENV_PROJECT_NAME = "COMPOSE_PROJECT_NAME";
    String ENV_COMPOSE_FILE = "COMPOSE_FILE";

    private static final String COMPOSE_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker-compose.exe" : "docker-compose";
    private static final String DOCKER_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker.exe" : "docker";

    private final List<File> composeFiles;
    private final String identifier;
    private String cmd = "";
    private Map<String, String> env = new HashMap<>();

    public DockerComposeExecutor(List<File> composeFiles, String identifier) {
        validateFileList(composeFiles);
        this.composeFiles = composeFiles;
        this.identifier = identifier;
    }

    public DockerComposeExecutor withCommand(String cmd) {
        this.cmd = cmd;
        return this;
    }

    public DockerComposeExecutor withEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    public void invokeCompose() {
        // bail out early
        if (!CommandLine.executableExists(COMPOSE_EXECUTABLE)) {
            throw new ContainerLaunchException("Local Docker Compose not found. Is " + COMPOSE_EXECUTABLE + " on the PATH?");
        }
        final Map<String, String> environment = Maps.newHashMap(env);
        environment.put(ENV_PROJECT_NAME, identifier);
        final Stream<String> absoluteDockerComposeFilePaths = composeFiles.stream().map(File::getAbsolutePath).map(Objects::toString);
        final String composeFileEnvVariableValue = absoluteDockerComposeFilePaths.collect(joining(File.pathSeparator + ""));
        log.debug("Set env COMPOSE_FILE={}", composeFileEnvVariableValue);
        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
        environment.put(ENV_COMPOSE_FILE, composeFileEnvVariableValue);
        log.info("Local Docker Compose is running command: {}", cmd);
        final List<String> command = Splitter.onPattern(" ").omitEmptyStrings().splitToList(COMPOSE_EXECUTABLE + " " + cmd);
        try {
            new ProcessExecutor().command(command).redirectOutput(Slf4jStream.of(log).asInfo()).redirectError(Slf4jStream.of(log).asError()).environment(environment).directory(pwd).exitValueNormal().executeNoTimeout();
            log.info("Docker Compose has finished running");
        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException("Local Docker Compose exited abnormally with code " + e.getExitValue() + " whilst running command: " + cmd);
        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker Compose command: " + cmd, e);
        }
    }

    public void invokeDocker() {
        // bail out early
        if (!CommandLine.executableExists(DOCKER_EXECUTABLE)) {
            throw new ContainerLaunchException("Local Docker not found. Is " + DOCKER_EXECUTABLE + " on the PATH?");
        }
        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
        log.info("Local Docker is running command: {}", cmd);
        final List<String> command = Splitter.onPattern(" ").omitEmptyStrings().splitToList(DOCKER_EXECUTABLE + " " + cmd);
        try {
            new ProcessExecutor().command(command).redirectOutput(Slf4jStream.of(log).asInfo()).redirectError(Slf4jStream.of(log).asError()).directory(pwd).exitValueNormal().executeNoTimeout();
            log.info("Docker has finished running");
        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException("Local Docker exited abnormally with code " + e.getExitValue() + " whilst running command: " + cmd);
        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker command: " + cmd, e);
        }
    }

    void validateFileList(List<File> composeFiles) {
        checkNotNull(composeFiles);
        checkArgument(!composeFiles.isEmpty(), "No docker compose file have been provided");
    }


}
