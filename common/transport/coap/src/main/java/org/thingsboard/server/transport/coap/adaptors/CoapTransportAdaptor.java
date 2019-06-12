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
package org.thingsboard.server.transport.coap.adaptors;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.PostAttributeMsg;
import org.thingsboard.server.gen.transport.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.ToServerRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.CoapTransportResource;

import java.util.UUID;
import java.util.Optional;

public interface CoapTransportAdaptor {

    PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound) throws AdaptorException;

    PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound) throws AdaptorException;

    ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, GetAttributeResponseMsg responseMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener session, ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException;

    Response convertToPublish(CoapTransportResource.CoapSessionListener coapSessionListener, ToServerRpcResponseMsg msg) throws AdaptorException;

}
