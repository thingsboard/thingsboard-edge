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
package org.thingsboard.rule.engine.analytics.incoming.state;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Created by ashvayka on 07.06.18.
 */

public interface TbIntervalState {

    void update(JsonElement value);

    boolean hasChangesToPersist();

    void clearChangesToPersist();

    boolean hasChangesToReport();

    void clearChangesToReport();

    String toValueJson(Gson gson, String outputValueKey);

    String toStateJson(Gson gson);

}
