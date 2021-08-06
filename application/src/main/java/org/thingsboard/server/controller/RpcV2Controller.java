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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.exception.ToErrorResponseEntity;
import org.thingsboard.server.service.rpc.RemoveRpcActorMsg;

import javax.annotation.Nullable;
import java.util.UUID;

import static org.thingsboard.server.common.data.DataConstants.RPC_DELETED;

@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.RPC_V2_URL_PREFIX)
@Slf4j
public class RpcV2Controller extends AbstractRpcController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/oneway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleOneWayDeviceRPCRequest(@PathVariable("deviceId") String deviceIdStr, @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(true, new DeviceId(UUID.fromString(deviceIdStr)), requestBody, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/twoway/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleTwoWayDeviceRPCRequest(@PathVariable("deviceId") String deviceIdStr, @RequestBody String requestBody) throws ThingsboardException {
        return handleDeviceRPCRequest(false, new DeviceId(UUID.fromString(deviceIdStr)), requestBody, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/persistent/{rpcId}", method = RequestMethod.GET)
    @ResponseBody
    public Rpc getPersistedRpc(@PathVariable("rpcId") String strRpc) throws ThingsboardException {
        checkParameter("RpcId", strRpc);
        try {
            RpcId rpcId = new RpcId(UUID.fromString(strRpc));
            return checkRpcId(rpcId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/persistent/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getPersistedRpcByDevice(@PathVariable("deviceId") String strDeviceId,
                                                                  @RequestParam int pageSize,
                                                                  @RequestParam int page,
                                                                  @RequestParam RpcStatus rpcStatus,
                                                                  @RequestParam(required = false) String textSearch,
                                                                  @RequestParam(required = false) String sortProperty,
                                                                  @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("DeviceId", strDeviceId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            DeviceId deviceId = new DeviceId(UUID.fromString(strDeviceId));
            final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            accessValidator.validate(getCurrentUser(), Operation.RPC_CALL, deviceId, new HttpValidationCallback(response, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                    PageData<Rpc> rpcCalls = rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
                    response.setResult(new ResponseEntity<>(rpcCalls, HttpStatus.OK));
                }

                @Override
                public void onFailure(Throwable e) {
                    ResponseEntity entity;
                    if (e instanceof ToErrorResponseEntity) {
                        entity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
                    } else {
                        entity = new ResponseEntity(HttpStatus.UNAUTHORIZED);
                    }
                    response.setResult(entity);
                }
            }));
            return response;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/persistent/{rpcId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteResource(@PathVariable("rpcId") String strRpc) throws ThingsboardException {
        checkParameter("RpcId", strRpc);
        try {
            RpcId rpcId = new RpcId(UUID.fromString(strRpc));
            Rpc rpc = checkRpcId(rpcId);

            if (rpc != null) {
                if (rpc.getStatus().equals(RpcStatus.QUEUED)) {
                    RemoveRpcActorMsg removeMsg = new RemoveRpcActorMsg(getTenantId(), rpc.getDeviceId(), rpc.getUuidId());
                    log.trace("[{}] Forwarding msg {} to queue actor!", rpc.getDeviceId(), rpc);
                    tbClusterService.pushMsgToCore(removeMsg, null);
                }

                rpcService.deleteRpc(getTenantId(), rpcId);

                TbMsg msg = TbMsg.newMsg(RPC_DELETED, rpc.getDeviceId(), TbMsgMetaData.EMPTY, JacksonUtil.toString(rpc));
                tbClusterService.pushMsgToRuleEngine(getTenantId(), rpc.getDeviceId(), msg, null);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
