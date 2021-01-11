///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
