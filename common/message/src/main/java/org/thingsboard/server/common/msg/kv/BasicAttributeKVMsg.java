/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.msg.kv;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Collections;
import java.util.List;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BasicAttributeKVMsg implements AttributesKVMsg {

    private static final long serialVersionUID = 1L;

    private final List<AttributeKvEntry> clientAttributes;
    private final List<AttributeKvEntry> sharedAttributes;
    private final List<AttributeKey> deletedAttributes;

    public static BasicAttributeKVMsg fromClient(List<AttributeKvEntry> attributes) {
        return new BasicAttributeKVMsg(attributes, Collections.emptyList(), Collections.emptyList());
    }

    public static BasicAttributeKVMsg fromShared(List<AttributeKvEntry> attributes) {
        return new BasicAttributeKVMsg(Collections.emptyList(), attributes, Collections.emptyList());
    }

    public static BasicAttributeKVMsg from(List<AttributeKvEntry> client, List<AttributeKvEntry> shared) {
        return new BasicAttributeKVMsg(client, shared, Collections.emptyList());
    }

    public static AttributesKVMsg fromDeleted(List<AttributeKey> shared) {
        return new BasicAttributeKVMsg(Collections.emptyList(), Collections.emptyList(), shared);
    }
}
