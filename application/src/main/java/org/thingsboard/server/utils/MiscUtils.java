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
package org.thingsboard.server.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.query.EntityFilterType;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;


/**
 * @author Andrew Shvayka
 */
public class MiscUtils {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    @SuppressWarnings("deprecation")
    public static HashFunction forName(String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "crc32":
                return Hashing.crc32();
            case "md5":
                return Hashing.md5();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

    public static String constructBaseUrl(HttpServletRequest request) {
        return String.format("%s://%s:%d",
                getScheme(request),
                getDomainName(request),
                getPort(request));
    }

    public static String getScheme(HttpServletRequest request){
        String scheme = request.getScheme();
        String forwardedProto = request.getHeader("x-forwarded-proto");
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        return scheme;
    }

    public static String getDomainName(HttpServletRequest request){
        return request.getServerName();
    }

    public static String getDomainNameAndPort(HttpServletRequest request){
        String domainName = getDomainName(request);
        String scheme = getScheme(request);
        int port = MiscUtils.getPort(request);
        if (needsPort(scheme, port)) {
            domainName += ":" + port;
        }
        return domainName;
    }

    private static boolean needsPort(String scheme, int port) {
        boolean isHttpDefault = "http".equals(scheme.toLowerCase()) && port == 80;
        boolean isHttpsDefault = "https".equals(scheme.toLowerCase()) && port == 443;
        return !isHttpDefault && !isHttpsDefault;
    }

    public static int getPort(HttpServletRequest request){
        String forwardedProto = request.getHeader("x-forwarded-proto");

        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
            }
        } else if (forwardedProto != null) {
            switch (forwardedProto) {
                case "http":
                    serverPort = 80;
                    break;
                case "https":
                    serverPort = 443;
                    break;
            }
        }
        return serverPort;
    }

    public static void updateDashboardFilterIfRequired(JsonNode entityAlias) {
        JsonNode filter = entityAlias.get("filter");
        updateFilterByTypeIfRequired(filter, EntityFilterType.ASSET_TYPE.getLabel());
        updateFilterByTypeIfRequired(filter, EntityFilterType.DEVICE_TYPE.getLabel());
        updateFilterByTypeIfRequired(filter, EntityFilterType.ENTITY_VIEW_TYPE.getLabel());
        updateFilterByTypeIfRequired(filter, EntityFilterType.EDGE_TYPE.getLabel());
    }

    private static void updateFilterByTypeIfRequired(JsonNode filter, String filterType) {
        if (filter == null || filter.get("type") == null) {
            return;
        }
        if (filterType.equals(filter.get("type").asText())) {
            if (filter.get(filterType) != null) {
                ArrayNode arrayNode = JacksonUtil.OBJECT_MAPPER.createArrayNode();
                arrayNode.add(filter.get(filterType).asText());
                ((ObjectNode) filter).set(filterType + "s", arrayNode);
                ((ObjectNode) filter).remove(filterType);
            }
        }
    }
}
