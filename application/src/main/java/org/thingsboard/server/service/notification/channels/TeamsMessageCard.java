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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TeamsMessageCard {
    @JsonProperty("@type")
    private final String type = "MessageCard";
    @JsonProperty("@context")
    private final String context = "http://schema.org/extensions";
    private String themeColor;
    private String summary;
    private String text;
    private List<Section> sections;
    private List<ActionCard> potentialAction;

    @Data
    public static class Section {
        private String activityTitle;
        private String activitySubtitle;
        private String activityImage;
        private List<Fact> facts;
        private boolean markdown;

        @Data
        public static class Fact {
            private final String name;
            private final String value;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ActionCard {
        @JsonProperty("@type")
        private String type; // ActionCard, OpenUri
        private String name;
        private List<Input> inputs; // for ActionCard
        private List<Action> actions; // for ActionCard
        private List<Target> targets;

        @Data
        public static class Input {
            @JsonProperty("@type")
            private String type; // TextInput, DateInput, MultichoiceInput
            private String id;
            private boolean isMultiple;
            private String title;
            private boolean isMultiSelect;

            @Data
            public static class Choice {
                private final String display;
                private final String value;
            }
        }

        @Data
        public static class Action {
            @JsonProperty("@type")
            private final String type; // HttpPOST
            private final String name;
            private final String target; // url
        }

        @Data
        public static class Target {
            private final String os;
            private final String uri;
        }
    }

}
