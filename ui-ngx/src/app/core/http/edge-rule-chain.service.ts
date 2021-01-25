///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  RuleChain
} from '@shared/models/rule-chain.models';
import { Edge } from "@shared/models/edge.models";

@Injectable({
  providedIn: 'root'
})
export class EdgeRuleChainService {

  constructor(
    private http: HttpClient
  ) { }

  public getRuleChains(pageLink: PageLink, config?: RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/ruleChains${pageLink.toQuery()}&type=EDGE`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEdgeRuleChains(edgeId: string, pageLink: PageLink, config?:RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/edge/${edgeId}/ruleChains${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config) )
  }

  public assignRuleChainToEdge(edgeId: string, ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/edge/${edgeId}/ruleChain/${ruleChainId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignRuleChainFromEdge(edgeId: string, ruleChainId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultRootEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/defaultRootEdge`, defaultHttpOptionsFromConfig(config));
  }

  public addDefaultEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/defaultEdge`, defaultHttpOptionsFromConfig(config));
  }

  public removeDefaultEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.delete<RuleChain>(`/api/ruleChain/${ruleChainId}/defaultEdge`, defaultHttpOptionsFromConfig(config));
  }

  public getDefaultEdgeRuleChains(config?: RequestConfig): Observable<Array<RuleChain>> {
    return this.http.get<Array<RuleChain>>(`/api/ruleChain/defaultEdgeRuleChains`, defaultHttpOptionsFromConfig(config));
  }

  public setRootRuleChain(edgeId: string, ruleChainId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/edge/${edgeId}/${ruleChainId}/root`, defaultHttpOptionsFromConfig(config));
  }

}
