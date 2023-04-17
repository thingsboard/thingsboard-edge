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
package org.thingsboard.server.service.sync.vc;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.service.sync.vc.GitRepository.Diff;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@ConditionalOnProperty(prefix = "vc", value = "git.service", havingValue = "local", matchIfMissing = true)
@Service
public class DefaultGitRepositoryService implements GitRepositoryService {

    @Value("${java.io.tmpdir}/repositories")
    private String defaultFolder;

    @Value("${vc.git.repositories-folder:${java.io.tmpdir}/repositories}")
    private String repositoriesFolder;

    private final Map<TenantId, GitRepository> repositories = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(repositoriesFolder)) {
            repositoriesFolder = defaultFolder;
        }
    }

    @Override
    public Set<TenantId> getActiveRepositoryTenants() {
        return new HashSet<>(repositories.keySet());
    }

    @Override
    public void prepareCommit(PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        String branch = commit.getBranch();
        try {
            repository.fetch();

            repository.createAndCheckoutOrphanBranch(commit.getWorkingBranch());
            repository.resetAndClean();

            if (repository.listRemoteBranches().contains(new BranchInfo(branch, false))) {
                repository.merge(branch);
            }
        } catch (IOException | GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    @Override
    public void deleteFolderContent(PendingCommit commit, String folder, boolean recursively) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        Path workDir = Path.of(repository.getDirectory());

        if (recursively) {
            Collection<File> dirs = FileUtils.listFilesAndDirs(workDir.toFile(), FalseFileFilter.FALSE, new NameFileFilter(".git").negate());
            for (File dir : dirs) {
                if (dir.getName().equals(folder)) {
                    FileUtils.deleteDirectory(dir);
                }
            }
        } else {
            FileUtils.deleteDirectory(Path.of(repository.getDirectory(), folder).toFile());
        }
    }

    @Override
    public void add(PendingCommit commit, String relativePath, String entityDataJson) throws IOException {
        GitRepository repository = checkRepository(commit.getTenantId());
        FileUtils.write(Path.of(repository.getDirectory(), relativePath).toFile(), entityDataJson, StandardCharsets.UTF_8);
    }

    @Override
    public VersionCreationResult push(PendingCommit commit) {
        GitRepository repository = checkRepository(commit.getTenantId());
        try {
            repository.add(".");

            VersionCreationResult result = new VersionCreationResult();
            GitRepository.Status status = repository.status();
            result.setAdded(status.getAdded().size());
            result.setModified(status.getModified().size());
            result.setRemoved(status.getRemoved().size());

            if (result.getAdded() > 0 || result.getModified() > 0 || result.getRemoved() > 0) {
                GitRepository.Commit gitCommit = repository.commit(commit.getVersionName(), commit.getAuthorName(), commit.getAuthorEmail());
                repository.push(commit.getWorkingBranch(), commit.getBranch());
                result.setVersion(toVersion(gitCommit));
            }
            return result;
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        } finally {
            cleanUp(commit);
        }
    }

    @SneakyThrows
    @Override
    public void cleanUp(PendingCommit commit) {
        log.debug("[{}] Cleanup tenant repository started.", commit.getTenantId());
        GitRepository repository = checkRepository(commit.getTenantId());
        try {
            repository.createAndCheckoutOrphanBranch(EntityId.NULL_UUID.toString());
        } catch (Exception e) {
            if (!e.getMessage().contains("NO_CHANGE")) {
                throw e;
            }
        }
        repository.resetAndClean();
        repository.deleteLocalBranchIfExists(commit.getWorkingBranch());
        log.debug("[{}] Cleanup tenant repository completed.", commit.getTenantId());
    }

    @Override
    public void abort(PendingCommit commit) {
        cleanUp(commit);
    }

    @Override
    public void fetch(TenantId tenantId) throws GitAPIException {
        var repository = repositories.get(tenantId);
        if (repository != null) {
            log.debug("[{}] Fetching tenant repository.", tenantId);
            repository.fetch();
            log.debug("[{}] Fetched tenant repository.", tenantId);
        }
    }

    @Override
    public String getFileContentAtCommit(TenantId tenantId, String relativePath, String versionId) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getFileContentAtCommit(relativePath, versionId);
    }

    @Override
    public List<Diff> getVersionsDiffList(TenantId tenantId, String path, String versionId1, String versionId2) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getDiffList(versionId1, versionId2, path);
    }

    @Override
    public String getContentsDiff(TenantId tenantId, String content1, String content2) throws IOException {
        GitRepository repository = checkRepository(tenantId);
        return repository.getContentsDiff(content1, content2);
    }

    @Override
    public List<BranchInfo> listBranches(TenantId tenantId) {
        GitRepository repository = checkRepository(tenantId);
        try {
            return repository.listRemoteBranches();
        } catch (GitAPIException gitAPIException) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(gitAPIException);
        }
    }

    private GitRepository checkRepository(TenantId tenantId) {
        return Optional.ofNullable(repositories.get(tenantId)).orElseThrow(() -> new IllegalStateException("Repository is not initialized"));
    }

    @Override
    public PageData<EntityVersion> listVersions(TenantId tenantId, String branch, String path, PageLink pageLink) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        return repository.listCommits(branch, path, pageLink).mapData(this::toVersion);
    }

    public Pattern buildPattern(EntityType entityType, boolean group) {
        String prefix = ".*";
        if (group) {
            prefix += "groups\\/";
        }
        prefix += entityType.name().toLowerCase() + "\\/";
        return Pattern.compile(prefix + "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}.json");
    }

    @Override
    public Stream<VersionedEntityInfo> listEntitiesAtVersion(TenantId tenantId, String versionId, String folder, EntityType entityType, boolean isGroup, boolean recursive) throws Exception {
        GitRepository repository = checkRepository(tenantId);
        if (entityType != null) {
            String path = recursive ? folder : StringUtils.emptyIfNull(folder) + entityType.name().toLowerCase();
            Pattern typePattern = buildPattern(entityType, false);
            Pattern groupPattern = buildPattern(entityType, true);
            return repository.listFilesAtCommit(versionId, path).stream()
                    .filter(filePath -> typePattern.matcher(filePath).matches())
                    .filter(filePath -> groupPattern.matcher(filePath).matches() == isGroup)
                    .map(filePath -> {
                        var parts = filePath.split("/");
                        var uuidStr = parts[parts.length - 1];
                        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, uuidStr.substring(0, 36));
                        return new VersionedEntityInfo(entityId, filePath);
                    })
                    .sorted(Comparator.comparing(VersionedEntityInfo::getPath, Comparator.comparingInt(String::length))
                            .thenComparing(VersionedEntityInfo::getPath, String::compareTo));
        } else {
            // Used to list all entities.
            Map<EntityType, Pattern> typePatterns = new HashMap<>();
            Map<EntityType, Pattern> groupPatterns = new HashMap<>();
            //TODO: we need only specific (the ones that we export) entity types but all other should not be present in the repository
            for (EntityType et : EntityType.values()) {
                groupPatterns.put(et, buildPattern(et, true));
                typePatterns.put(et, buildPattern(et, false));
            }
            return repository.listFilesAtCommit(versionId, folder).stream()
                    .map(filePath -> {
                        for (var pair : groupPatterns.entrySet()) {
                            if (pair.getValue().matcher(filePath).matches()) {
                                return Pair.of(EntityType.ENTITY_GROUP, filePath);
                            }
                        }
                        for (var pair : typePatterns.entrySet()) {
                            if (pair.getValue().matcher(filePath).matches()) {
                                return Pair.of(pair.getKey(), filePath);
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull)
                    .map(pair -> {
                        var parts = pair.getSecond().split("/");
                        var uuidStr = parts[parts.length - 1];
                        EntityId entityId = EntityIdFactory.getByTypeAndUuid(pair.getFirst(), uuidStr.substring(0, 36));
                        return new VersionedEntityInfo(entityId, pair.getSecond());
                    })
                    .sorted(Comparator.comparing(VersionedEntityInfo::getPath, Comparator.comparingInt(String::length))
                            .thenComparing(VersionedEntityInfo::getPath, String::compareTo));

        }
    }

    @Override
    public void testRepository(TenantId tenantId, RepositorySettings settings) throws Exception {
        Path testDirectory = Path.of(repositoriesFolder, "repo-test-" + UUID.randomUUID());
        GitRepository.test(settings, testDirectory.toFile());
    }

    @Override
    public void initRepository(TenantId tenantId, RepositorySettings settings) throws Exception {
        testRepository(tenantId, settings);

        clearRepository(tenantId);
        log.debug("[{}] Init tenant repository started.", tenantId);
        Path repositoryDirectory = Path.of(repositoriesFolder, tenantId.getId().toString());
        GitRepository repository;
        if (Files.exists(repositoryDirectory)) {
            FileUtils.forceDelete(repositoryDirectory.toFile());
        }

        Files.createDirectories(repositoryDirectory);
        repository = GitRepository.clone(settings, repositoryDirectory.toFile());
        repositories.put(tenantId, repository);
        log.debug("[{}] Init tenant repository completed.", tenantId);
    }

    @Override
    public RepositorySettings getRepositorySettings(TenantId tenantId) throws Exception {
        var gitRepository = repositories.get(tenantId);
        return gitRepository != null ? gitRepository.getSettings() : null;
    }

    @Override
    public void clearRepository(TenantId tenantId) throws IOException {
        GitRepository repository = repositories.get(tenantId);
        if (repository != null) {
            log.debug("[{}] Clear tenant repository started.", tenantId);
            FileUtils.deleteDirectory(new File(repository.getDirectory()));
            repositories.remove(tenantId);
            log.debug("[{}] Clear tenant repository completed.", tenantId);
        }
    }

    private EntityVersion toVersion(GitRepository.Commit commit) {
        return new EntityVersion(commit.getTimestamp(), commit.getId(), commit.getMessage(), this.getAuthor(commit));
    }

    private String getAuthor(GitRepository.Commit commit) {
        String author = String.format("<%s>", commit.getAuthorEmail());
        if (StringUtils.isNotBlank(commit.getAuthorName())) {
            author = String.format("%s %s", commit.getAuthorName(), author);
        }
        return author;
    }

    public static EntityId fromRelativePath(String path) {
        EntityType entityType = EntityType.valueOf(StringUtils.substringBefore(path, "/").toUpperCase());
        String entityId = StringUtils.substringBetween(path, "/", ".json");
        return EntityIdFactory.getByTypeAndUuid(entityType, entityId);
    }
}
