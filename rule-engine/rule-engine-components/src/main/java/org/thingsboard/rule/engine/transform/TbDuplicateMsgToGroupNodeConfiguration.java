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
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.EntityGroupId;

@Data
public class TbDuplicateMsgToGroupNodeConfiguration extends TbDuplicateMsgNodeConfiguration implements NodeConfiguration {

    private EntityGroupId entityGroupId;

    private boolean entityGroupIsMessageOriginator;

    @Override
    public TbDuplicateMsgToGroupNodeConfiguration defaultConfiguration() {
        TbDuplicateMsgToGroupNodeConfiguration configuration = new TbDuplicateMsgToGroupNodeConfiguration();
        configuration.setEntityGroupIsMessageOriginator(true);
        return configuration;
    }
}
