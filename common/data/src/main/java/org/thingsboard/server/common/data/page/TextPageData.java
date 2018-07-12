/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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

import java.util.List;
import java.util.UUID;

import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.UUIDBased;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TextPageData<T extends SearchTextBased<? extends UUIDBased>> {

    private final List<T> data;
    private final TextPageLink nextPageLink;
    private final boolean hasNext;

    public TextPageData(List<T> data, TextPageLink pageLink) {
        super();
        this.data = data;
        int limit = pageLink.getLimit();
        if (data != null && data.size() == limit) {
            int index = data.size()-1;
            UUID idOffset = data.get(index).getId().getId();
            String textOffset = data.get(index).getSearchText();
            nextPageLink = new TextPageLink(limit, pageLink.getTextSearch(), idOffset, textOffset);
            hasNext = true;
        } else {
            nextPageLink = null;
            hasNext = false;
        }
    }
    
    @JsonCreator
    public TextPageData(@JsonProperty("data") List<T> data,
                        @JsonProperty("nextPageLink") TextPageLink nextPageLink,
                        @JsonProperty("hasNext") boolean hasNext) {
        this.data = data;
        this.nextPageLink = nextPageLink;
        this.hasNext = hasNext;
    }

    public List<T> getData() {
        return data;
    }

    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasNext;
    }
    
    public TextPageLink getNextPageLink() {
        return nextPageLink;
    }

}
