/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;

public class PageDataIterable<T extends SearchTextBased<? extends UUIDBased>> implements Iterable<T>, Iterator<T> {

    private final FetchFunction<T> function;
    private final int fetchSize;

    private List<T> currentItems;
    private int currentIdx;
    private boolean hasNextPack;
    private TextPageLink nextPackLink;
    private boolean initialized;

    public PageDataIterable(FetchFunction<T> function, int fetchSize) {
        super();
        this.function = function;
        this.fetchSize = fetchSize;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        if(!initialized){
            fetch(new TextPageLink(fetchSize));
            initialized = true;
        }
        if(currentIdx == currentItems.size()){
            if(hasNextPack){
                fetch(nextPackLink);
            }
        }
        return currentIdx < currentItems.size();
    }

    private void fetch(TextPageLink link) {
        TextPageData<T> pageData = function.fetch(link);
        currentIdx = 0;
        currentItems = pageData.getData();
        hasNextPack = pageData.hasNext();
        nextPackLink = pageData.getNextPageLink();
    }

    @Override
    public T next() {
        if(!hasNext()){
            throw new NoSuchElementException();
        }
        return currentItems.get(currentIdx++);
    }

    public static interface FetchFunction<T extends SearchTextBased<? extends UUIDBased>> {

        TextPageData<T> fetch(TextPageLink link);

    }
}
