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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@RequiredArgsConstructor
public class TbConverterRepoService {

    @Value("${service.converter.repo.owner}")
    private String repoOwner;

    @Value("${service.converter.repo.name}")
    private String repoName;

    @Value("${service.converter.repo.token}")
    private String repoToken;

    private static final String githubApiUrl = "https://api.github.com";

    private String converterGitHubUrl;


    private final RestTemplate restTemplate = new RestTemplate();
    private final String rootPath = "VENDORS";


    @PostConstruct
    public void init() {
        converterGitHubUrl = String.format("%s/repos/%s/%s", githubApiUrl, repoOwner, repoName);
    }

    public ArrayNode getVendorsByIntegrationType(String integrationType) throws ThingsboardException {
        try {
            ArrayNode vendorsLogo = JacksonUtil.newArrayNode();
            TbVendorsIntegration vendors = getVendorsIntegration(integrationType);
            vendors.getFiltersSet().forEach(v -> {
                String pathLogo = (String) vendors.getTreeList().stream().filter(n -> n.toString().contains(v.toString()) && n.toString().contains("logo")).findFirst().orElse("");
                vendors.getTreeNodes().forEach(n -> {
                    if (n.get("path").asText().equals(pathLogo)) {
                        ObjectNode vendorLogo = JacksonUtil.newObjectNode();
                        vendorLogo.put("name", (String) v);
                        vendorLogo.put("logo", n.get("url").asText());
                        vendorsLogo.add(vendorLogo);
                    }
                });
            });
            return vendorsLogo;
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    /**
     *
     * @param integrationType
     * @param vendorName
     * @return [{"name": "WS202", info: {url, label, description}, photo: {urlFile}]
     * @throws ThingsboardException
     */
    public ArrayNode getVendorModelsByIntegrationType(String integrationType, String vendorName) throws ThingsboardException {
        ArrayNode modelsInfo = JacksonUtil.newArrayNode();
        TbVendorsIntegration models = getModelsIntegration(integrationType, vendorName);
        models.getFiltersSet().forEach(m -> {
            String pathLogo = (String) models.getTreeList().stream().filter(n -> n.toString().contains(m.toString()) && n.toString().contains("photo")).findFirst().orElse("");
            models.getTreeNodes().forEach(n -> {
                if (n.get("path").asText().equals(pathLogo)) {
                    ObjectNode modelPhoto = JacksonUtil.newObjectNode();
                    modelPhoto.put("name", (String) m);
                    ObjectNode info = getFileJson(null, vendorName, (String) m, null, "info.json");
                    modelPhoto.put("info", info);
                    modelPhoto.put("photo", n.get("url").asText());
                    modelsInfo.add(modelPhoto);
                }
            });
        });
        return modelsInfo;
    }


    public String getFileString(String integrationType, String vendorName, String model, String decoderType, String fileName) throws ThingsboardException {
        String filePath = decoderType != null ?
                String.format("%s/%s/%s/%s/%s/%s", rootPath, vendorName, model, integrationType, decoderType, fileName) :
                String.format("%s/%s/%s/%s", rootPath, vendorName, model, fileName);
        TbRepoContent content = responseRestTemplateGet(createUriBuilderContent(filePath), TbRepoContent.class);
        if (content != null && "base64".equals(content.getEncoding())) {
            byte[] decodedBytes = Base64.decodeBase64(content.getContent());
            return new String(decodedBytes, StandardCharsets.UTF_8);
        }
        return content != null ? content.getContent() : null;
    }

    public ObjectNode getFileJson(String integrationType, String vendorName, String model, String decoderType, String fileName) {
        try {
            String fileStr = getFileString(integrationType, vendorName, model, decoderType, fileName);
            return JacksonUtil.fromString(fileStr, ObjectNode.class);
        } catch (Exception e) {
            try {
                throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } catch (ThingsboardException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private TbVendorsIntegration getVendorsIntegration(String integrationType) throws ThingsboardException {
        TbVendorsIntegration vendors = getTreeToLists();
        List vendorsByIntegrationType = (List) vendors.getTreeList().stream().filter(n -> n.toString().contains(integrationType)).collect(Collectors.toList());
        vendorsByIntegrationType.forEach(p -> vendors.getFiltersSet().add(p.toString().split("/")[1]));
        return vendors;
    }

    private TbVendorsIntegration getModelsIntegration(String integrationType, String vendorName) throws ThingsboardException {
        TbVendorsIntegration models = getTreeToLists();
        List modelsByVendorIntegrationType = (List) models.getTreeList().stream().filter(n -> n.toString().contains(integrationType) && n.toString().contains(vendorName)).collect(Collectors.toList());
        modelsByVendorIntegrationType.forEach(p -> models.getFiltersSet().add(p.toString().split("/")[2]));
        return models;
    }

    private TbVendorsIntegration getTreeToLists() throws ThingsboardException {
        String tree = responseRestTemplateGet(createUriBuilderTree(), String.class);
        JsonNode root = JacksonUtil.toJsonNode(tree);
        JsonNode treeNodes = root.path("tree");
        List treeList = treeNodes.findValuesAsText("path");
        Set vendorsSet = new HashSet();
        return new TbVendorsIntegration(treeNodes, treeList, vendorsSet);
    }

    private <T> T responseRestTemplateGet(UriComponentsBuilder uriBuilder, Class<T> responseType) throws ThingsboardException {
        try {
            HttpEntity<String> entity = createHeaders();

            ResponseEntity<T> response;
            if (responseType == null) {
                response = restTemplate.exchange(
                        uriBuilder.toUriString(),
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {
                        }
                );
            } else {
                response = restTemplate.exchange(
                        uriBuilder.toUriString(),
                        HttpMethod.GET,
                        entity,
                        responseType
                );
            }
            return response.getBody();
        } catch (Exception e) {
            throw new ThingsboardException(e.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private HttpEntity<String> createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (repoToken != null && !repoToken.isEmpty()) {
            headers.set("Authorization", "token " + repoToken);
        }
        return new HttpEntity<>(headers);

    }

    private UriComponentsBuilder createUriBuilderTree() {
        return UriComponentsBuilder.fromHttpUrl(converterGitHubUrl + "/git/trees/master?recursive=1");
    }

    private UriComponentsBuilder createUriBuilderContent(String pathDir) {
        return UriComponentsBuilder.fromHttpUrl(converterGitHubUrl + "/contents/" + pathDir);
    }
}

