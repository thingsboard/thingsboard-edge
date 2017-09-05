/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.common.msg.core;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.session.MsgType;

public class BasicUpdateAttributesRequest extends BasicRequest implements UpdateAttributesRequest {

    private static final long serialVersionUID = 1L;

    private final Set<AttributeKvEntry> data;

    public BasicUpdateAttributesRequest() {
        this(DEFAULT_REQUEST_ID);
    }

    public BasicUpdateAttributesRequest(Integer requestId) {
        super(requestId);
        this.data = new LinkedHashSet<>();
    }

    public void add(AttributeKvEntry entry) {
        this.data.add(entry);
    }

    public void add(Collection<AttributeKvEntry> entries) {
        this.data.addAll(entries);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.POST_ATTRIBUTES_REQUEST;
    }

    @Override
    public Set<AttributeKvEntry> getAttributes() {
        return data;
    }

    @Override
    public String toString() {
        return "BasicUpdateAttributesRequest [data=" + data + "]";
    }

}
