///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Injectable, Type, ɵComponentDef, ɵNG_COMP_DEF } from '@angular/core';
import { forkJoin, from, Observable, of } from 'rxjs';
import { CommonModule } from '@angular/common';
import { mergeMap } from 'rxjs/operators';
import { guid } from '@core/utils';
import { getFlexLayoutModule } from '@shared/legacy/flex-layout.models';

@Injectable({
    providedIn: 'root'
})
export class DynamicComponentFactoryService {

  constructor() {
  }

  public createDynamicComponent<T>(
                     componentType: Type<T>,
                     template: string,
                     imports?: Type<any>[],
                     preserveWhitespaces?: boolean,
                     styles?: string[]): Observable<Type<T>> {
    return forkJoin({flexLayoutModule: getFlexLayoutModule(), compiler: from(import('@angular/compiler'))}).pipe(
      mergeMap((data) => {
        let componentImports: Type<any>[] = [CommonModule, data.flexLayoutModule];
        if (imports) {
          componentImports = [...componentImports, ...imports];
        }
        const comp = this.createAndCompileDynamicComponent(componentType, template, componentImports, preserveWhitespaces, styles);
        return of(comp.type);
      })
    );
  }

  public destroyDynamicComponent<T>(_componentType: Type<T>) {
  }

  public getComponentDef<T>(componentType: Type<T>): ɵComponentDef<T> {
    return componentType[ɵNG_COMP_DEF];
  }

  private createAndCompileDynamicComponent<T>(componentType: Type<T>, template: string, imports: Type<any>[],
                                              preserveWhitespaces?: boolean, styles?: string[]): ɵComponentDef<T> {
    // noinspection AngularMissingOrInvalidDeclarationInModule
    const comp = Component({
      template,
      imports,
      preserveWhitespaces,
      styles,
      standalone: true,
      selector: 'tb-dynamic-component#' + guid()
    })(componentType);
    // Trigger component compilation
    return comp[ɵNG_COMP_DEF];
  }

}
