/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.apache.cassandra.io.sstable.format;

import com.google.common.base.CharMatcher;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.db.RowIndexEntry;
import org.apache.cassandra.db.SerializationHeader;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.io.sstable.format.Version;
import org.apache.cassandra.io.sstable.format.big.BigFormat;

/**
 * Provides the accessors to data on disk.
 */
public interface SSTableFormat
{
    static boolean enableSSTableDevelopmentTestMode = Boolean.getBoolean("cassandra.test.sstableformatdevelopment");


    Version getLatestVersion();
    Version getVersion(String version);

    SSTableWriter.Factory getWriterFactory();
    SSTableReader.Factory getReaderFactory();

    RowIndexEntry.IndexSerializer<?> getIndexSerializer(CFMetaData cfm, Version version, SerializationHeader header);

    public static enum Type
    {
        //Used internally to refer to files with no
        //format flag in the filename
        LEGACY("big", BigFormat.instance),

        //The original sstable format
        BIG("big", BigFormat.instance);

        public final org.apache.cassandra.io.sstable.format.SSTableFormat info;
        public final String name;

        public static Type current()
        {
            return BIG;
        }

        @SuppressWarnings("deprecation")
        private Type(String name, org.apache.cassandra.io.sstable.format.SSTableFormat info)
        {
            //Since format comes right after generation
            //we disallow formats with numeric names
            // We have removed this check for compatibility with the embedded cassandra used for tests.
            assert !CharMatcher.digit().matchesAllOf(name);

            this.name = name;
            this.info = info;
        }

        public static Type validate(String name)
        {
            for (Type valid : Type.values())
            {
                //This is used internally for old sstables
                if (valid == LEGACY)
                    continue;

                if (valid.name.equalsIgnoreCase(name))
                    return valid;
            }

            throw new IllegalArgumentException("No Type constant " + name);
        }
    }
}
