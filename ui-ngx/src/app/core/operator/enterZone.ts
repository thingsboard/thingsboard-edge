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


import { MonoTypeOperatorFunction, Observable, Operator, Subscriber } from 'rxjs';

export type EnterZoneSignature<T> = (zone: { run: (fn: any) => any }) => Observable<T>;

export function enterZone<T>(zone: { run: (fn: any) => any }): MonoTypeOperatorFunction<T> {
  return (source: Observable<T>) => {
    return source.lift(new EnterZoneOperator(zone));
  };
}

export class EnterZoneOperator<T> implements Operator<T, T> {
  constructor(private zone: { run: (fn: any) => any }) { }

  call(subscriber: Subscriber<T>, source: any): any {
    return source._subscribe(new EnterZoneSubscriber(subscriber, this.zone));
  }
}

class EnterZoneSubscriber<T> extends Subscriber<T> {
  constructor(destination: Subscriber<T>, private zone: { run: (fn: any) => any }) {
    super(destination);
  }

  protected _next(value: T) {
    this.zone.run(() => this.destination.next(value));
  }
}
