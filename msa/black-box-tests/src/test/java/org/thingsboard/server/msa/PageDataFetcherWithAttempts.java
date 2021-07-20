/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa;

import org.junit.Assert;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

public class PageDataFetcherWithAttempts<T> {

    private final FetchFunction<T> function;
    private final int numberOfAttempts;
    private final int expectedSize;

    public PageDataFetcherWithAttempts(FetchFunction<T> function, int numberOfAttempts, int expectedSize) {
        super();
        this.function = function;
        this.numberOfAttempts = numberOfAttempts;
        this.expectedSize = expectedSize;
    }

    public PageData<T> fetchData() {
        boolean found = false;
        int attempt = 0;
        PageData<T> pageData = null;
        do {
            try {
                pageData = function.fetch(new PageLink(100));
                if (pageData != null && pageData.getData().size() == expectedSize) {
                    found = true;
                }
            } catch (Exception ignored1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored2) {}
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored2) {}
            attempt++;
            if (attempt > numberOfAttempts) {
                break;
            }
        } while (!found);
        Assert.assertNotNull("PageData can't be null", pageData);
        Assert.assertNotNull("PageData Data can't be null", pageData.getData());
        Assert.assertTrue("Number of fetched entities count " + pageData.getData().size() + " doesn't meet expected count " + expectedSize, found);
        return pageData;
    }

    public interface FetchFunction<T> {

        PageData<T> fetch(PageLink link);

    }
}
