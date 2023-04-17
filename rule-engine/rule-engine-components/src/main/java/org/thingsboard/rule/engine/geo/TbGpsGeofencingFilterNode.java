/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.geo;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "gps geofencing filter",
        configClazz = TbGpsGeofencingFilterNodeConfiguration.class,
        relationTypes = {"True", "False"},
        nodeDescription = "Filter incoming messages by GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from the incoming message and checks them according to configured perimeter. </br>" +
                "Configuration:</br></br>" +
                "<ul>" +
                "<li>Latitude key name - name of the message field that contains location latitude;</li>" +
                "<li>Longitude key name - name of the message field that contains location longitude;</li>" +
                "<li>Perimeter type - Polygon or Circle;</li>" +
                "<li>Fetch perimeter from message metadata - checkbox to load perimeter from message metadata; " +
                "   Enable if your perimeter is specific to device/asset and you store it as device/asset attribute;</li>" +
                "<li>Perimeter key name - name of the metadata key that stores perimeter information;</li>" +
                "<li>For Polygon perimeter type: <ul>" +
                "    <li>Polygon definition - string that contains array of coordinates in the following format: [[lat1, lon1],[lat2, lon2],[lat3, lon3], ... , [latN, lonN]]</li>" +
                "</ul></li>" +
                "<li>For Circle perimeter type: <ul>" +
                "   <li>Center latitude - latitude of the circle perimeter center;</li>" +
                "   <li>Center longitude - longitude of the circle perimeter center;</li>" +
                "   <li>Range - value of the circle perimeter range, double-precision floating-point value;</li>" +
                "   <li>Range units - one of: Meter, Kilometer, Foot, Mile, Nautical Mile;</li>" +
                "</ul></li></ul></br>" +
                "Rule node will use default metadata key names, if the \"Fetch perimeter from message metadata\" is enabled and \"Perimeter key name\" is not configured. " +
                "Default metadata key names for polygon perimeter type is \"perimeter\". Default metadata key names for circle perimeter are: \"centerLatitude\", \"centerLongitude\", \"range\", \"rangeUnit\"." +
                "</br></br>" +
                "Structure of the circle perimeter definition (stored in server-side attribute, for example):" +
                "</br></br>" +
                "{\"latitude\":  48.198618758582384, \"longitude\": 24.65322245153503, \"radius\":  100.0, \"radiusUnit\": \"METER\" }" +
                "</br></br>" +
                "Available radius units: METER, KILOMETER, FOOT, MILE, NAUTICAL_MILE;",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeGpsGeofencingConfig")
public class TbGpsGeofencingFilterNode extends AbstractGeofencingNode<TbGpsGeofencingFilterNodeConfiguration> {

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ctx.tellNext(msg, checkMatches(msg) ? "True" : "False");
    }

    @Override
    protected Class<TbGpsGeofencingFilterNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingFilterNodeConfiguration.class;
    }
}
