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
package org.thingsboard.server.dao.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.NoXssValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class KvUtils {

    private static final Cache<String, Boolean> validatedKeys;

    static {
        validatedKeys = Caffeine.newBuilder()
                .weakKeys()
                .expireAfterAccess(60, TimeUnit.MINUTES)
                .maximumSize(100000).build();
    }

    public static void validate(List<? extends KvEntry> tsKvEntries) {
        tsKvEntries.forEach(KvUtils::validate);
    }

    public static void validate(KvEntry tsKvEntry) {
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }

        String key = tsKvEntry.getKey();

        if (key == null) {
            throw new DataValidationException("Key can't be null");
        }

        if (validatedKeys.getIfPresent(key) != null) {
            return;
        }

        if (!NoXssValidator.isValid(key)) {
            throw new DataValidationException("Validation error: key is malformed");
        }
        if (key.length() > 255) {
            throw new DataValidationException("Validation error: key length must be equal or less than 255");
        }

        validatedKeys.put(key, Boolean.TRUE);
    }
}
