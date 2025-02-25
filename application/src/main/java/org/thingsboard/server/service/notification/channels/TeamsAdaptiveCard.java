/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.notification.channels;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.dao.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @link <a href="https://adaptivecards.io/designer/">AdaptiveCard Designer</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamsAdaptiveCard {
    private String type = "message";
    private List<Attachment> attachments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String contentType = "application/vnd.microsoft.card.adaptive";
        private AdaptiveCard content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdaptiveCard {
        @JsonProperty("$schema")
        private final String schema = "http://adaptivecards.io/schemas/adaptive-card.json";
        private final String type = "AdaptiveCard";
        private BackgroundImage backgroundImage;
        @JsonProperty("body")
        private List<TextBlock> textBlocks = new ArrayList<>();
        private List<ActionOpenUrl> actions = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class BackgroundImage {
        private String url;
        private final String fillMode = "repeat";

        public BackgroundImage(String color) {
            // This is the only one way how to specify color the custom color for the card
            url = ImageUtils.getEmbeddedBase64EncodedImg(color);
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextBlock {
        private final String type = "TextBlock";
        private String text;
        private String weight = "Normal";
        private String size = "Medium";
        private String spacing = "None";
        private String color = "#FFFFFF";
        private final boolean wrap = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionOpenUrl {
        private final String type = "Action.OpenUrl";
        private String title;
        private String url;
    }

}