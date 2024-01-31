///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  ConnectorType,
  RPCTemplate
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { AttributeService } from '@core/http/attribute.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { isLiteralObject } from '@app/core/utils';

@Component({
  selector: 'tb-gateway-service-rpc-connector-templates',
  templateUrl: './gateway-service-rpc-connector-templates.component.html',
  styleUrls: ['./gateway-service-rpc-connector-templates.component.scss']
})
export class GatewayServiceRPCConnectorTemplatesComponent implements OnInit {

  @Input()
  connectorType: ConnectorType;

  @Input()
  ctx: WidgetContext;

  @Output()
  saveTemplate: EventEmitter<any> = new EventEmitter();

  @Output()
  useTemplate: EventEmitter<any> = new EventEmitter();

  @Input()
  rpcTemplates: Array<RPCTemplate>;

  public readonly originalOrder = (): number => 0;
  public readonly isObject = (value: any) => isLiteralObject(value);

  constructor(private attributeService: AttributeService) {
  }

  ngOnInit() {
  }

  public applyTemplate($event: Event, template: RPCTemplate): void {
    $event.stopPropagation();
    this.useTemplate.emit(template);
  }

  public deleteTemplate($event: Event, template: RPCTemplate): void {
    $event.stopPropagation();
    const index = this.rpcTemplates.findIndex(data => {
      return data.name == template.name;
    })
    this.rpcTemplates.splice(index, 1);
    const key = `${this.connectorType}_template`;
    this.attributeService.saveEntityAttributes(
      {
        id: this.ctx.defaultSubscription.targetDeviceId,
        entityType: EntityType.DEVICE
      }
      , AttributeScope.SERVER_SCOPE, [{
        key,
        value: this.rpcTemplates
      }]).subscribe(() => {
    })
  }
}
