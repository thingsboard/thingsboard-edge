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
package org.thingsboard.server.service.converter;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.LibraryConvertersInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;
import org.thingsboard.server.service.sync.vc.GitRepositoryService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultConverterLibraryService implements ConverterLibraryService {

    private static Map<String, LibraryConvertersInfo> libraryConvertersInfoMap;
    private final GitRepositoryService gitRepositoryService;

    @Value("${integrations.converters.library.enabled:true}")
    private boolean enabled;
    @Value("${integrations.converters.library.url:https://github.com/thingsboard/data-converters.git}")
    private String repoUrl;
    @Value("${integrations.converters.library.fetch_frequency:24}")
    private int fetchFrequencyHours;

    private ScheduledExecutorService executor;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void init() throws Exception {
        if (!enabled) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("data-converters-library"));
        RepositorySettings settings = new RepositorySettings();
        settings.setRepositoryUri(repoUrl);

        executor.execute(() -> {
            try {
                gitRepositoryService.initRepository(TenantId.SYS_TENANT_ID, settings);
                collectLibraryConvertersInfo();
                log.info("Initialized data converters repository");
            } catch (Throwable e) {
                log.error("Failed to init data converters repository for settings {}", settings, e);
            }
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                gitRepositoryService.fetch(TenantId.SYS_TENANT_ID);
                collectLibraryConvertersInfo();
            } catch (Throwable e) {
                log.error("Failed to fetch data converters repository", e);
            }
        }, fetchFrequencyHours, fetchFrequencyHours, TimeUnit.HOURS);
    }

    @PreDestroy
    public void preDestroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public List<Vendor> getVendors(IntegrationType integrationType) {
        return listFiles("VENDORS", 1, true).stream()
                .filter(vendorDir -> {
                    Set<String> integrationTypes = listFiles(vendorDir.path(), 3, true).stream()
                            .map(RepoFile::name)
                            .collect(Collectors.toSet());
                    return integrationTypes.contains(integrationType.getDirectory());
                })
                .map(vendorDir -> {
                    String logoFile = findFile(vendorDir.path(), 2, "logo");
                    return new Vendor(vendorDir.name(), getGithubRawContentUrl(logoFile));
                })
                .toList();
    }

    @Override
    public List<Model> getVendorModels(IntegrationType integrationType, String converterType, String vendorName) {
        return listFiles("VENDORS/" + vendorName, 3, true).stream()
                .filter(integrationDir -> {
                    if (!integrationDir.name().equals(integrationType.getDirectory())) {
                        return false;
                    }
                    if (StringUtils.isEmpty(converterType)) {
                        return true;
                    }
                    List<String> converterTypes = listFiles(integrationDir.path(), 4, true).stream()
                            .map(RepoFile::name).toList();
                    return converterTypes.contains(converterType);
                })
                .map(integrationDir -> Path.of(integrationDir.path()).getParent())
                .map(modelPath -> {
                    String name = modelPath.getFileName().toString();
                    JsonNode modelInfo = JacksonUtil.toJsonNode(getFileContent(modelPath + "/info.json"));
                    String photoFile = findFile(modelPath.toString(), 3, "photo");
                    String photo = getGithubRawContentUrl(photoFile);
                    return new Model(name, modelInfo, photo);
                })
                .toList();
    }


    @Override
    public String getConverter(IntegrationType integrationType, String converterType, String vendorName, String model) {
        return getFileContent(getConverterDir(integrationType, converterType, vendorName, model) + "/converter.json");
    }

    @Override
    public String getConverterMetadata(IntegrationType integrationType, String converterType, String vendorName, String model) {
        return getFileContent(getConverterDir(integrationType, converterType, vendorName, model) + "/metadata.json");
    }

    @Override
    public String getPayload(IntegrationType integrationType, String converterType, String vendorName, String model) {
        return getFileContent(getConverterDir(integrationType, converterType, vendorName, model) + "/payload.json");
    }

    @Override
    public Map<String, LibraryConvertersInfo> getLibraryConvertersInfo() {
        return libraryConvertersInfoMap;
    }

    private String getGithubRawContentUrl(String path) {
        if (path == null) {
            return "";
        }
        return StringUtils.removeEnd(repoUrl, ".git") + "/blob/master/" + path + "?raw=true";
    }

    private String findFile(String dir, int depth, String prefix) {
        return listFiles(dir, depth, false).stream()
                .filter(file -> file.name().startsWith(prefix))
                .findFirst().map(RepoFile::path).orElse(null);
    }

    private List<RepoFile> listFiles(String path, int depth, boolean isDirectory) {
        if (!enabled) {
            throw new IllegalArgumentException("Data converters library is disabled");
        }
        return gitRepositoryService.listFiles(TenantId.SYS_TENANT_ID, "HEAD", path, depth).stream()
                .filter(file -> file.isDirectory() == isDirectory)
                .toList();
    }

    private String getFileContent(String path) {
        if (!enabled) {
            throw new IllegalArgumentException("Data converters library is disabled");
        }
        try {
            return gitRepositoryService.getFileContentAtCommit(TenantId.SYS_TENANT_ID, path, "HEAD");
        } catch (Exception e) {
            log.warn("Failed to get file content for path {}: {}", path, e.getMessage());
            return "{}";
        }
    }

    private String getConverterDir(IntegrationType integrationType, String converterType, String vendorName, String model) {
        return Path.of("VENDORS", vendorName, model, integrationType.getDirectory(), converterType).toString();
    }

    private void collectLibraryConvertersInfo() {
        libraryConvertersInfoMap = listFiles("VENDORS", 4, true).stream()
                .collect(Collectors.groupingBy(
                        repoFile -> Path.of(repoFile.path()).getParent().getFileName().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                converters -> {
                                    boolean hasUplink = converters.stream()
                                            .anyMatch(converterPath -> "uplink".equals(Path.of(converterPath.path()).getFileName().toString()));
                                    boolean hasDownlink = converters.stream()
                                            .anyMatch(converterPath -> "downlink".equals(Path.of(converterPath.path()).getFileName().toString()));
                                    return new LibraryConvertersInfo(hasUplink, hasDownlink);
                                }
                        )
                ));
    }

}
