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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Base64Utils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.util.ImageUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class SystemImagesMigrator {

    private static final Path dataDir = Path.of(
            "/home/*/thingsboard-ce/application/src/main/data"
    );
    private static final Path imagesDir = dataDir.resolve("images");
    private static final Path widgetBundlesDir = dataDir.resolve("json").resolve("system").resolve("widget_bundles");
    private static final Path widgetTypesDir = dataDir.resolve("json").resolve("system").resolve("widget_types");
    private static final Path demoDashboardsDir = dataDir.resolve("json").resolve("demo").resolve("dashboards");

    public static void main(String[] args) throws Exception {
        Files.list(widgetTypesDir).forEach(file -> {
            ObjectNode widgetTypeJson = (ObjectNode) JacksonUtil.toJsonNode(file.toFile());
            updateWidget(widgetTypeJson);
            saveJson(file, widgetTypeJson);
        });

        Files.list(widgetBundlesDir).forEach(file -> {
            JsonNode widgetsBundleDescriptorJson = JacksonUtil.toJsonNode(file.toFile());
            ObjectNode widgetsBundleJson = (ObjectNode) widgetsBundleDescriptorJson.get("widgetsBundle");
            updateWidgetsBundle(widgetsBundleJson);
            saveJson(file, widgetsBundleDescriptorJson);
        });

        Files.list(demoDashboardsDir).forEach(file -> {
            ObjectNode dashboardJson = (ObjectNode) JacksonUtil.toJsonNode(file.toFile());
            updateDashboard(dashboardJson);
            saveJson(file, dashboardJson);
        });
    }

    public static void updateWidgetsBundle(ObjectNode widgetsBundleJson) {
        String imageLink = getText(widgetsBundleJson, "image");
        widgetsBundleJson.put("image", inlineImage(imageLink, "widget_bundles"));
    }

    public static void updateWidget(ObjectNode widgetJson) {
        String previewImageLink = widgetJson.get("image").asText();
        widgetJson.put("image", inlineImage(previewImageLink, "widgets"));

        ObjectNode descriptor = (ObjectNode) widgetJson.get("descriptor");
        JsonNode defaultConfig = JacksonUtil.toJsonNode(descriptor.get("defaultConfig").asText());
        updateWidgetConfig(defaultConfig, "widgets");
        descriptor.put("defaultConfig", defaultConfig.toString());
    }

    public static void updateDashboard(ObjectNode dashboardJson) {
        String image = getText(dashboardJson, "image");
        dashboardJson.put("image", inlineImage(image, "dashboards"));

        dashboardJson.get("configuration").get("widgets").elements().forEachRemaining(widgetConfig -> {
            updateWidgetConfig(widgetConfig.get("config"), "dashboards");
        });
    }

    private static void updateWidgetConfig(JsonNode widgetConfigJson, String directory) {
        ObjectNode widgetSettings = (ObjectNode) widgetConfigJson.get("settings");
        ArrayNode markerImages = (ArrayNode) widgetSettings.get("markerImages");
        if (markerImages != null && !markerImages.isEmpty()) {
            for (int i = 0; i < markerImages.size(); i++) {
                markerImages.set(i, inlineImage(markerImages.get(i).asText(), directory));
            }
        }

        String mapImage = getText(widgetSettings, "mapImageUrl");
        if (mapImage != null) {
            widgetSettings.put("mapImageUrl", inlineImage(mapImage, directory));
        }

        String backgroundImage = getText(widgetSettings, "backgroundImageUrl");
        if (backgroundImage != null) {
            widgetSettings.put("backgroundImageUrl", inlineImage(backgroundImage, directory));
        }

        JsonNode backgroundConfigNode = widgetSettings.get("background");
        if (backgroundConfigNode != null && backgroundConfigNode.isObject()) {
            ObjectNode backgroundConfig = (ObjectNode) backgroundConfigNode;
            if ("imageUrl".equals(getText(backgroundConfig, "type"))) {
                String imageLink = getText(backgroundConfig, "imageUrl");
                if (imageLink != null && imageLink.startsWith("/api/images")) {
                    backgroundConfig.put("imageBase64", inlineImage(imageLink, directory));
                    backgroundConfig.set("imageUrl", null);
                    backgroundConfig.put("type", "image");
                }
            }
        }
    }

    @SneakyThrows
    private static String inlineImage(String url, String subDir) {
        if (url != null && url.startsWith("/api/images")) {
            String imageKey = StringUtils.substringAfterLast(url, "/");
            Path file = imagesDir.resolve(subDir).resolve(imageKey);
            String mediaType = ImageUtils.fileExtensionToMediaType(StringUtils.substringAfterLast(imageKey, "."));
            return "data:" + mediaType + ";base64," + Base64Utils.encodeToString(Files.readAllBytes(file));
        } else {
            return url;
        }
    }

    private static String getText(JsonNode jsonNode, String field) {
        return Optional.ofNullable(jsonNode.get(field))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText).orElse(null);
    }

    @SneakyThrows
    private static void saveJson(Path file, JsonNode json) {
        Files.write(file, JacksonUtil.toPrettyString(json).getBytes(StandardCharsets.UTF_8));
    }

}
