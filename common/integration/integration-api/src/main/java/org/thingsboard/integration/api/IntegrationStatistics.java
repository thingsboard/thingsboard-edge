/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.integration.api;

import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;

@ToString
public class IntegrationStatistics {

    private AtomicLong messagesProcessed;
    private AtomicLong errorsOccurred;

    public IntegrationStatistics() {
        messagesProcessed = new AtomicLong(0);
        errorsOccurred = new AtomicLong(0);
    }

    public void incMessagesProcessed() {
        messagesProcessed.incrementAndGet();
    }

    public void incErrorsOccurred() {
        errorsOccurred.incrementAndGet();
    }

    public long getMessagesProcessed() {
        return messagesProcessed.get();
    }

    public long getErrorsOccurred() {
        return errorsOccurred.get();
    }

}
