/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

@ToString
public class TextPageLink extends BasePageLink implements Serializable {

    private static final long serialVersionUID = -4189954843653250480L;

    @Getter private final String textSearch;
    @Getter private final String textSearchBound;
    @Getter private final String textOffset;

    public TextPageLink(int limit) {
        this(limit, null, null, null);
    }

    public TextPageLink(int limit, String textSearch) {
        this(limit, textSearch, null, null);
    }

    public TextPageLink(int limit, String textSearch, UUID idOffset, String textOffset) {
        super(limit, idOffset);
        this.textSearch = textSearch != null ? textSearch.toLowerCase() : null;
        this.textSearchBound = nextSequence(this.textSearch);
        this.textOffset = textOffset != null ? textOffset.toLowerCase() : null;
    }

    @JsonCreator
    public TextPageLink(@JsonProperty("limit") int limit,
                        @JsonProperty("textSearch") String textSearch,
                        @JsonProperty("textSearchBound") String textSearchBound,
                        @JsonProperty("textOffset") String textOffset,
                        @JsonProperty("idOffset") UUID idOffset) {
        super(limit, idOffset);
        this.textSearch = textSearch;
        this.textSearchBound = textSearchBound;
        this.textOffset = textOffset;
        this.idOffset = idOffset;
    }

    private static String nextSequence(String input) {
        if (input != null && input.length() > 0) {
            char[] chars = input.toCharArray();
            int i = chars.length - 1;
            while (i >= 0 && ++chars[i--] == Character.MIN_VALUE) ;
            if (i == -1 && (chars.length == 0 || chars[0] == Character.MIN_VALUE)) {
                char buf[] = Arrays.copyOf(input.toCharArray(), input.length() + 1);
                buf[buf.length - 1] = Character.MIN_VALUE;
                return new String(buf);
            }
            return new String(chars);
        } else {
            return null;
        }
    }

}
