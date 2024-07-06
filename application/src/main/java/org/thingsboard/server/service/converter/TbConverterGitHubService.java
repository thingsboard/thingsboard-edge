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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TbCoreComponent
@Service
@RequiredArgsConstructor
public class TbConverterGitHubService {

    @Value("${service.converter.repo.owner}")
    private String repoOwner;

    @Value("${service.converter.repo.name}")
    private String repoName;

    @Value("${service.converter.repo.token}")
    private String repoToken;

    private static final String githubApiUrl = "https://api.github.com";

    private String converterGitHubUrl;


    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init () {
        converterGitHubUrl = String.format("%s/repos/%s/%s/contents/", githubApiUrl, repoOwner, repoName);

    }

    public TbGitHubContent[]  listFiles(String pathDir) throws ThingsboardException {
        try {
            String path = pathDir == null ? "VENDORS" : pathDir.startsWith("VENDORS") ? pathDir : "VENDORS" + "/" + pathDir;
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(converterGitHubUrl + path);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + repoToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<TbGitHubContent[]> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    TbGitHubContent[].class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    public List<Map<String, Object>> getFilesFromDirectory(String path) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(converterGitHubUrl + path);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + repoToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                });
        return response.getBody();
    }

    public TbFileNode buildFileTree(String pathDir) throws ThingsboardException {
        try {
            String path = pathDir == null ? "VENDORS" : pathDir.startsWith("VENDORS") ? pathDir : "VENDORS" + "/" + pathDir;
            List<Map<String, Object>> files = getFilesFromDirectory(path);
            File file = new File(path);
            TbFileNode root = new TbFileNode(file.getName(), path, true);
            Map<String, TbFileNode> pathNodeMap = new HashMap<>();
            pathNodeMap.put(path, root);
            buildTreeRecursive(files, pathNodeMap);
            return root;
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private void buildTreeRecursive(List<Map<String, Object>> files, Map<String, TbFileNode> pathNodeMap) {
        for (Map<String, Object> file : files) {
            String filePath = (String) file.get("path");
            String fileName = (String) file.get("name");
            boolean isDirectory = "dir".equals(file.get("type"));
            TbFileNode fileNode = new TbFileNode(fileName, filePath, isDirectory);
            String parentPath = filePath.substring(0, filePath.lastIndexOf('/'));
            TbFileNode parentNode = pathNodeMap.get(parentPath);
            if (parentNode != null) {
                parentNode.addChild(fileNode);
                if (isDirectory) {
                    List<Map<String, Object>> subFiles = getFilesFromDirectory(filePath);
                    pathNodeMap.put(filePath, fileNode);
                    buildTreeRecursive(subFiles, pathNodeMap);
                }
            }
        }
    }

    public void fetchDirectoryContents(String path, List<Map<String, Object>> allFiles) {
        List<Map<String, Object>> files = getFilesFromDirectory(path);
        for (Map<String, Object> file : files) {
            allFiles.add(file);
            if ("dir".equals(file.get("type"))) {
                fetchDirectoryContents((String) file.get("path"), allFiles);
            }
        }
    }

    public List<Map<String, Object>> getAllFilesFromDirectory(String pathDir) {
        String path = pathDir == null ? "VENDORS" : pathDir.startsWith("VENDORS") ? pathDir : "VENDORS" + "/" + pathDir;

        List<Map<String, Object>> allFiles = new ArrayList<>();
        fetchDirectoryContents(path, allFiles);
        return allFiles;
    }

    public String getFileContentJson(String filePath) throws ThingsboardException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(converterGitHubUrl + filePath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + repoToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TbGitHubContent> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    TbGitHubContent.class
            );

            TbGitHubContent content = response.getBody();
            if (content != null && "base64".equals(content.getEncoding())) {
                byte[] decodedBytes = Base64.decodeBase64(content.getContent());
                return new String(decodedBytes, StandardCharsets.UTF_8);
            }

            return content != null ? content.getContent() : null;
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    public byte[] getFileContentPng(String filePath) throws ThingsboardException {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(converterGitHubUrl + filePath);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + repoToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TbGitHubContent> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    TbGitHubContent.class
            );

            TbGitHubContent content = response.getBody();
            if (content != null && "base64".equals(content.getEncoding())) {
                return Base64.decodeBase64(content.getContent());
            }

            return null;
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }
}
