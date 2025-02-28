/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.mqtt;

import java.util.concurrent.Future;
import io.netty.buffer.ByteBuf;

public interface MqttHandler {
    /**
    * Changing ListenableFuture to Future allows you to choose CompletableFuture,
    * which gives developers the freedom to choose the orchestration method. 
    * CompletableFuture is a newer, more evolved version that eliminates callback hell,
    * is easier to use, and comes with the JDK. jdk 1.8 was previously used with the Before JDK1.8, 
    * use ListenableFuture, after that, it is recommended to use CompletableFuture.
    * ListenableFuture It's still written that way.{@link MqttMessageListener#onMessage(topic, payload)}
    * public ListenableFuture<Void> onMessage(String topic, ByteBuf message) {
    *        log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
    *        events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
    *       return Futures.immediateVoidFuture();
    *    }
    * CompletableFuture It's like this.
    * public CompletableFuture<Void> onMessage(String topic, ByteBuf message) {
    *        log.info("MQTT message [{}], topic [{}]", message.toString(StandardCharsets.UTF_8), topic);
    *       events.add(new MqttEvent(topic, message.toString(StandardCharsets.UTF_8)));
    *       return CompletableFuture.completedFuture(null);
    *    }
    * This change does not affect the system's current use of ListenableFuture so that it is free to choose between ListenableFuture or 
    * CompletableFuture in new development.
    */
    Future<Void> onMessage(String topic, ByteBuf payload);
}
