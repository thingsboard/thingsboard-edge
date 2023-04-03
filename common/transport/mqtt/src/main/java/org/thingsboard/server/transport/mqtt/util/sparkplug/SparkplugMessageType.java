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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

/**
 * An enumeration of Sparkplug MQTT message types.  The type provides an indication as to what the MQTT Payload of 
 * message will contain.
 */
public enum SparkplugMessageType {
	
	/**
	 * Birth certificate for MQTT Edge of Network (EoN) Nodes.
	 */
	NBIRTH,
	
	/**
	 * Death certificate for MQTT Edge of Network (EoN) Nodes.
	 */
	NDEATH,
	
	/**
	 * Birth certificate for MQTT Devices.
	 */
	DBIRTH,
	
	/**
	 * Death certificate for MQTT Devices.
	 */
	DDEATH,
	
	/**
	 * Edge of Network (EoN) Node data message.
	 */
	NDATA,
	
	/**
	 * Device data message.
	 */
	DDATA,
	
	/**
	 * Edge of Network (EoN) Node command message.
	 */
	NCMD,
	
	/**
	 * Device command message.
	 */
	DCMD,
	
	/**
	 * Critical application state message.
	 */
	STATE,
	
	/**
	 * Device record message.
	 */
	DRECORD,
	
	/**
	 * Edge of Network (EoN) Node record message.
	 */
	NRECORD;
	
	public static SparkplugMessageType parseMessageType(String type) throws ThingsboardException {
		for (SparkplugMessageType messageType : SparkplugMessageType.values()) {
			if (messageType.name().equals(type)) {
				return messageType;
			}
		}
		throw new ThingsboardException("Invalid message type: " + type, ThingsboardErrorCode.INVALID_ARGUMENTS);
	}
	public static String messageName(SparkplugMessageType type) {
		return STATE.equals(type) ? "sparkplugConnectionState" : type.name();
	}
	
	public boolean isDeath() {
		return this.equals(DDEATH) || this.equals(NDEATH);
	}
	
	public boolean isCommand() {
		return this.equals(DCMD) || this.equals(NCMD);
	}
	
	public boolean isData() {
		return this.equals(DDATA) || this.equals(NDATA);
	}
	
	public boolean isBirth() {
		return this.equals(DBIRTH) || this.equals(NBIRTH);
	}
	
	public boolean isRecord() {
		return this.equals(DRECORD) || this.equals(NRECORD);
	}
}
