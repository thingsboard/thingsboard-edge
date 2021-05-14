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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;

import java.util.Collection;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.convertPathFromObjectIdToIdVer;

@Slf4j
public class LwM2mServerListener {

    private final LwM2mTransportMsgHandler service;

    public LwM2mServerListener(LwM2mTransportMsgHandler service) {
        this.service = service;
    }

    public final RegistrationListener registrationListener = new RegistrationListener() {
        /**
         * Register – запрос, представленный в виде POST /rd?…
         */
        @Override
        public void registered(Registration registration, Registration previousReg,
                               Collection<Observation> previousObservations) {
            service.onRegistered(registration, previousObservations);
        }

        /**
         * Update – представляет из себя CoAP POST запрос на URL, полученный в ответ на Register.
         */
        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                            Registration previousRegistration) {
            service.updatedReg(updatedRegistration);
        }

        /**
         * De-register (CoAP DELETE) – отправляется клиентом в случае инициирования процедуры выключения.
         */
        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                 Registration newReg) {
            service.unReg(registration, observations);
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {
        @Override
        public void onSleeping(Registration registration) {
            log.info("onSleeping");
            service.onSleepingDev(registration);
        }

        @Override
        public void onAwake(Registration registration) {
            log.info("onAwake");
            service.onAwakeDev(registration);
        }
    };

    public final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
            String msg = String.format("%s:  Cancel Observation  %s.", LOG_LW2M_INFO, observation.getPath());
            service.sendLogsToThingsboard(msg, observation.getRegistrationId());
            log.warn(msg);
        }

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            if (registration != null) {
                try {
                    service.onUpdateValueAfterReadResponse(registration, convertPathFromObjectIdToIdVer(observation.getPath().toString(),
                            registration), response, null);
                } catch (Exception e) {
                    log.error("Observation/Read onResponse", e);

                }
            }
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            log.error(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(), observation.getPath()), error);
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
            String msg = String.format("%s: Successful start newObservation  %s.", LOG_LW2M_INFO,
                    observation.getPath());
            service.sendLogsToThingsboard(msg, registration.getId());
            log.trace(msg);
        }
    };

}
