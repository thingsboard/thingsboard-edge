/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.gson.JsonElement;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
abstract class TbBaseIntervalState implements TbIntervalState {

    private boolean hasChangesToPersist = true;
    private boolean hasChangesToReport = true;

    @Override
    public void update(JsonElement value) {
        if(doUpdate(value)){
            hasChangesToPersist = true;
            hasChangesToReport = true;
        }
    }

    @Override
    public boolean hasChangesToReport(){
        return hasChangesToReport;
    }

    @Override
    public boolean hasChangesToPersist(){
        return hasChangesToPersist;
    }

    @Override
    public void clearChangesToPersist(){
        hasChangesToPersist = false;
    }

    @Override
    public void clearChangesToReport(){
        hasChangesToReport = false;
    }

    protected abstract boolean doUpdate(JsonElement value);
}
