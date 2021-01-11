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
package org.thingsboard.integration.api.converter;

import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

/**
 * Created by ashvayka on 02.12.17.
 */
public interface TBDownlinkDataConverter extends TBDataConverter {

    List<DownlinkData> convertDownLink(ConverterContext context, List<TbMsg> downLinkMsgs, IntegrationMetaData metadata) throws Exception;

}
