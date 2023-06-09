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
package org.thingsboard.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;

@SuppressWarnings({"WeakerAccess", "unused", "SimplifiableIfStatement", "StringBufferReplaceableByString"})
public final class MqttLastWill {

    private final String topic;
    private final String message;
    private final boolean retain;
    private final MqttQoS qos;

    public MqttLastWill(String topic, String message, boolean retain, MqttQoS qos) {
        if(topic == null){
            throw new NullPointerException("topic");
        }
        if(message == null){
            throw new NullPointerException("message");
        }
        if(qos == null){
            throw new NullPointerException("qos");
        }
        this.topic = topic;
        this.message = message;
        this.retain = retain;
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetain() {
        return retain;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public static MqttLastWill.Builder builder(){
        return new MqttLastWill.Builder();
    }

    public static final class Builder {

        private String topic;
        private String message;
        private boolean retain;
        private MqttQoS qos;

        public String getTopic() {
            return topic;
        }

        public Builder setTopic(String topic) {
            if(topic == null){
                throw new NullPointerException("topic");
            }
            this.topic = topic;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public Builder setMessage(String message) {
            if(message == null){
                throw new NullPointerException("message");
            }
            this.message = message;
            return this;
        }

        public boolean isRetain() {
            return retain;
        }

        public Builder setRetain(boolean retain) {
            this.retain = retain;
            return this;
        }

        public MqttQoS getQos() {
            return qos;
        }

        public Builder setQos(MqttQoS qos) {
            if(qos == null){
                throw new NullPointerException("qos");
            }
            this.qos = qos;
            return this;
        }

        public MqttLastWill build(){
            return new MqttLastWill(topic, message, retain, qos);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MqttLastWill that = (MqttLastWill) o;

        if (retain != that.retain) return false;
        if (!topic.equals(that.topic)) return false;
        if (!message.equals(that.message)) return false;
        return qos == that.qos;

    }

    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (retain ? 1 : 0);
        result = 31 * result + qos.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttLastWill{");
        sb.append("topic='").append(topic).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", retain=").append(retain);
        sb.append(", qos=").append(qos.name());
        sb.append('}');
        return sb.toString();
    }
}
