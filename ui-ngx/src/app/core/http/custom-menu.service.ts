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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CustomMenu } from '@shared/models/custom-menu.models';
import { Observable, Subject } from 'rxjs';
import { mergeMap, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class CustomMenuService {

  private customMenu: CustomMenu = null;

  private customMenuChanged: Subject<CustomMenu> = new Subject<CustomMenu>();

  public customMenuChanged$: Observable<CustomMenu> = this.customMenuChanged.asObservable();

  constructor(
    private http: HttpClient
  ) {}

  public getCustomMenu(): CustomMenu {
    return this.customMenu;
  }

  public loadCustomMenu(): Observable<CustomMenu> {
    return this.http.get<CustomMenu>('/api/customMenu/customMenu').pipe(
      tap((customMenu) => {
        this.customMenu = customMenu;
      })
    );
  }

  public getCurrentCustomMenu(): Observable<CustomMenu> {
    return this.http.get<CustomMenu>('/api/customMenu/currentCustomMenu');
  }

  public saveCustomMenu(customMenu: CustomMenu): Observable<any> {
    return this.http.post<CustomMenu>('/api/customMenu/customMenu', customMenu).pipe(
      mergeMap(() => {
        return this.loadCustomMenu().pipe(
          tap((loaded) => {
            this.customMenuChanged.next(customMenu);
          }
        ))
      })
    );
  }
}
