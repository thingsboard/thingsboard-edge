/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
