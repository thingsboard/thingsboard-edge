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
package org.thingsboard.server.edqs.data.dp;

import lombok.Getter;
import lombok.SneakyThrows;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.edqs.repo.TbBytePool;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CompressedStringDataPoint extends AbstractDataPoint {

    public static final int MIN_STR_SIZE_TO_COMPRESS = 512;
    @Getter
    private final byte[] value;

    public static final AtomicInteger cnt = new AtomicInteger();
    public static final AtomicLong uncompressedLength = new AtomicLong();
    public static final AtomicLong compressedLength = new AtomicLong();

    @SneakyThrows
    public CompressedStringDataPoint(long ts, String value) {
        super(ts);
        cnt.incrementAndGet();
        uncompressedLength.addAndGet(value.getBytes(StandardCharsets.UTF_8).length);
        this.value = TbBytePool.intern(Snappy.compress(value));
        compressedLength.addAndGet(this.value.length);
    }

    @Override
    public DataType getType() {
        return DataType.STRING;
    }

    @SneakyThrows
    @Override
    public String getStr() {
        return Snappy.uncompressString(value);
    }

    @SneakyThrows
    @Override
    public String valueToString() {
        return Snappy.uncompressString(value);
    }

}
