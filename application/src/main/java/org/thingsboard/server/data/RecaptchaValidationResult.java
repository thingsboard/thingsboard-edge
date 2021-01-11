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
package org.thingsboard.server.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class RecaptchaValidationResult {

    private boolean success;
    private List<RecaptchaErrorCode> errorCodes = new ArrayList<>();

    @JsonCreator
    public RecaptchaValidationResult(
            @JsonProperty("success") boolean success,
            @JsonProperty("error-codes") List<RecaptchaErrorCode> errorCodes
    ) {
        this.success = success;
        this.errorCodes = errorCodes == null ? new ArrayList<RecaptchaErrorCode>() : errorCodes;
    }

    public boolean isSuccess() {
        return success;
    }

    @JsonIgnore
    public boolean isFailure() {
        return !success;
    }

    public List<RecaptchaErrorCode> getErrorCodes() {
        return unmodifiableList(errorCodes);
    }

    public boolean hasError(RecaptchaErrorCode error) {
        return errorCodes.contains(error);
    }

    @Override
    public String toString() {
        return "RecaptchaValidationResult{" +
                "success=" + success +
                ", errorCodes=" + errorCodes +
                '}';
    }

}
