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
import { ComponentDescriptor, ComponentType } from '@shared/models/component-descriptor.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { RuleNodeType } from '@shared/models/rule-node.models';
import { RuleChainType } from '@shared/models/rule-chain.models';

@Injectable({
  providedIn: 'root'
})
export class ComponentDescriptorService {

  private componentsByTypeByRuleChainType: Map<RuleChainType, Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>> =
    new Map<RuleChainType, Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>>();
  private componentsByClazz: Map<string, ComponentDescriptor> = new Map<string, ComponentDescriptor>();

  constructor(
    private http: HttpClient
  ) {
  }

  public getComponentDescriptorsByType(componentType: ComponentType, ruleChainType: RuleChainType, config?: RequestConfig): Observable<Array<ComponentDescriptor>> {
    if (!this.componentsByTypeByRuleChainType.get(ruleChainType)) {
      this.componentsByTypeByRuleChainType.set(ruleChainType, new Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>());
    }
    const existing = this.componentsByTypeByRuleChainType.get(ruleChainType).get(componentType);
    if (existing) {
      return of(existing);
    } else {
      return this.http.get<Array<ComponentDescriptor>>(`/api/components/${componentType}&ruleChainType=${ruleChainType}`, defaultHttpOptionsFromConfig(config)).pipe(
        map((componentDescriptors) => {
          this.componentsByTypeByRuleChainType.get(ruleChainType).set(componentType, componentDescriptors);
          componentDescriptors.forEach((componentDescriptor) => {
            this.componentsByClazz.set(componentDescriptor.clazz, componentDescriptor);
          });
          return componentDescriptors;
        })
      );
    }
  }

  public getComponentDescriptorsByTypes(componentTypes: Array<ComponentType>, ruleChainType: RuleChainType, config?: RequestConfig): Observable<Array<ComponentDescriptor>> {
    if (!this.componentsByTypeByRuleChainType.get(ruleChainType)) {
      this.componentsByTypeByRuleChainType.set(ruleChainType, new Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>());
    }
    let result: ComponentDescriptor[] = [];
    for (let i = componentTypes.length - 1; i >= 0; i--) {
      const componentType = componentTypes[i];
      const componentDescriptors = this.componentsByTypeByRuleChainType.get(ruleChainType).get(componentType);
      if (componentDescriptors) {
        result = result.concat(componentDescriptors);
        componentTypes.splice(i, 1);
      }
    }
    if (!componentTypes.length) {
      return of(result);
    } else {
      return this.http.get<Array<ComponentDescriptor>>(`/api/components?ruleChainType=${ruleChainType}&componentTypes=${componentTypes.join(',')}`,
        defaultHttpOptionsFromConfig(config)).pipe(
        map((componentDescriptors) => {
          componentDescriptors.forEach((componentDescriptor) => {
            let componentsList = this.componentsByTypeByRuleChainType.get(ruleChainType).get(componentDescriptor.type);
            if (!componentsList) {
              componentsList = new Array<ComponentDescriptor>();
              this.componentsByTypeByRuleChainType.get(ruleChainType).set(componentDescriptor.type, componentsList);
            }
            componentsList.push(componentDescriptor);
            this.componentsByClazz.set(componentDescriptor.clazz, componentDescriptor);
          });
          result = result.concat(componentDescriptors);
          return result;
        })
      );
    }
  }

  public getComponentDescriptorByClazz(componentDescriptorClazz: string, config?: RequestConfig): Observable<ComponentDescriptor> {
    const existing = this.componentsByClazz.get(componentDescriptorClazz);
    if (existing) {
      return of(existing);
    } else {
      return this.http.get<ComponentDescriptor>(`/api/component/${componentDescriptorClazz}`, defaultHttpOptionsFromConfig(config)).pipe(
        map((componentDescriptor) => {
          this.componentsByClazz.set(componentDescriptorClazz, componentDescriptor);
          return componentDescriptor;
        })
      );
    }
  }
}
