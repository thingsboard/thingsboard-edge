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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link LwM2mInstanceEnabler} for the Server (1) object.
 */
@Slf4j
public class Lwm2mServer extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Lwm2mServer.class);

    private final static List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 6, 7, 8, 22);

    private int shortServerId;
    private long lifetime;
    private Long defaultMinPeriod;
    private Long defaultMaxPeriod;
    private EnumSet<BindingMode> binding;
    private BindingMode preferredTransport;
    private boolean notifyWhenDisable;

    public Lwm2mServer() {
        // should only be used at bootstrap time
    }

    public Lwm2mServer(int shortServerId, long lifetime, EnumSet<BindingMode> binding, boolean notifyWhenDisable,
                       BindingMode preferredTransport) {
        this.shortServerId = shortServerId;
        this.lifetime = lifetime;
        this.binding = binding;
        this.notifyWhenDisable = notifyWhenDisable;
        this.preferredTransport = preferredTransport;
    }

    public Lwm2mServer(int shortServerId, long lifetime) {
        this(shortServerId, lifetime, EnumSet.of(BindingMode.U), false, BindingMode.U);
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        if (!identity.isSystem())
            LOG.debug("Read on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        switch (resourceid) {
        case 0: // short server ID
            return ReadResponse.success(resourceid, shortServerId);

        case 1: // lifetime
            return ReadResponse.success(resourceid, lifetime);

        case 2: // default min period
            if (null == defaultMinPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMinPeriod);

        case 3: // default max period
            if (null == defaultMaxPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMaxPeriod);

        case 6: // notification storing when disable or offline
            return ReadResponse.success(resourceid, notifyWhenDisable);

        case 7: // binding
            return ReadResponse.success(resourceid, BindingMode.toString(binding));

        case 22: // preferred transport
            if (preferredTransport == null)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, preferredTransport.toString());

        default:
            return super.read(identity, resourceid);
        }
    }

    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceid, LwM2mResource value) {
        if (!identity.isSystem())
            log.debug("Write on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        switch (resourceid) {
        case 0:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            int previousShortServerId = shortServerId;
            shortServerId = ((Long) value.getValue()).intValue();
            if (previousShortServerId != shortServerId)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 1:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            long previousLifetime = lifetime;
            lifetime = (Long) value.getValue();
            if (previousLifetime != lifetime)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 2:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMinPeriod = defaultMinPeriod;
            defaultMinPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMinPeriod, defaultMinPeriod))
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 3:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMaxPeriod = defaultMaxPeriod;
            defaultMaxPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMaxPeriod, defaultMaxPeriod))
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 6: // notification storing when disable or offline
            if (value.getType() != Type.BOOLEAN) {
                return WriteResponse.badRequest("invalid type");
            }
            boolean previousNotifyWhenDisable = notifyWhenDisable;
            notifyWhenDisable = (boolean) value.getValue();
            if (previousNotifyWhenDisable != notifyWhenDisable)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 7: // binding
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                EnumSet<BindingMode> previousBinding = binding;
                binding = BindingMode.parse((String) value.getValue());
                if (!Objects.equals(previousBinding, binding))
                    fireResourceChange(resourceid);
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }
        case 22: // preferredTransport
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                BindingMode previousPreferedTransport = preferredTransport;
                preferredTransport = BindingMode.valueOf((String) value.getValue());
                if (!Objects.equals(previousPreferedTransport, preferredTransport))
                    fireResourceChange(resourceid);
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }

        default:
            return super.write(identity, replace, resourceid, value);
        }
    }

    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
        log.info("Execute on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);
        if (resourceid == 8) {
            getLwM2mClient().triggerRegistrationUpdate(identity);
            return ExecuteResponse.success();
        } else if (resourceid == 9) {
            boolean success = getLwM2mClient().triggerClientInitiatedBootstrap(true);
            if (success) {
                return ExecuteResponse.success();
            }
            else {
                return ExecuteResponse.badRequest("probably no bootstrap server configured");
            }
        } else {
            return super.execute(identity, resourceid, params);
        }
    }

    @Override
    public void reset(int resourceid) {
        switch (resourceid) {
        case 2:
            defaultMinPeriod = null;
            break;
        case 3:
            defaultMaxPeriod = null;
            break;
        default:
            super.reset(resourceid);
        }
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
