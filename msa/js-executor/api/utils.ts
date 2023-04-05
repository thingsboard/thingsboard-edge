///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import Long from 'long';
import uuidParse from 'uuid-parse';

export function toUUIDString(mostSigBits: string, leastSigBits: string): string {
    const msbBytes = Long.fromValue(mostSigBits, false).toBytes(false);
    const lsbBytes = Long.fromValue(leastSigBits, false).toBytes(false);
    const uuidBytes = msbBytes.concat(lsbBytes);
    return uuidParse.unparse(uuidBytes as any);
}

export function UUIDFromBuffer(buf: Buffer): string {
    return uuidParse.unparse(buf);
}

export function UUIDToBits(uuidString: string): [string, string] {
    const bytes = Array.from(uuidParse.parse(uuidString));
    const msb = Long.fromBytes(bytes.slice(0, 8), false, false).toString();
    const lsb = Long.fromBytes(bytes.slice(-8), false, false).toString();
    return [msb, lsb];
}

export function isString(value: any): boolean {
    return typeof value === 'string';
}

export function parseJsErrorDetails(err: any): string | undefined {
    if (!err) {
        return undefined;
    }
    let details = err.name + ': ' + err.message;
    if (err.stack) {
        const lines = err.stack.split('\n');
        if (lines && lines.length) {
            const line = lines[0];
            const split = line.split(':');
            if (split && split.length === 2) {
                if (!isNaN(split[1])) {
                    details += ' in at line number ' + split[1];
                }
            }
        }
    }
    return details;
}

export function isNotUUID(candidate: string) {
    return candidate.length != 36 || !candidate.includes('-');
}
