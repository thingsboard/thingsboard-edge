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

import { Component, OnInit } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  EntityDetailsField,
  entityDetailsTranslations,
  FetchTo
} from '@home/components/rule-node/rule-node-config.models';

@Component({
  selector: 'tb-enrichment-node-entity-details-config',
  templateUrl: './entity-details-config.component.html',
  styleUrls: []
})

export class EntityDetailsConfigComponent extends RuleNodeConfigurationComponent implements OnInit {

  entityDetailsConfigForm: FormGroup;

  public predefinedValues = [];

  constructor(public translate: TranslateService,
              private fb: FormBuilder) {
    super();
    for (const field of Object.keys(EntityDetailsField)) {
      this.predefinedValues.push({
        value: EntityDetailsField[field],
        name: this.translate.instant(entityDetailsTranslations.get(EntityDetailsField[field]))
      });
    }
  }

  ngOnInit() {
    super.ngOnInit();
  }

  protected configForm(): FormGroup {
    return this.entityDetailsConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let fetchTo: FetchTo;
    if (isDefinedAndNotNull(configuration?.addToMetadata)) {
      if (configuration.addToMetadata) {
        fetchTo = FetchTo.METADATA;
      } else {
        fetchTo = FetchTo.DATA;
      }
    } else {
      if (configuration?.fetchTo) {
        fetchTo = configuration.fetchTo;
      } else {
        fetchTo = FetchTo.DATA;
      }
    }

    return {
      detailsList: isDefinedAndNotNull(configuration?.detailsList) ? configuration.detailsList : null,
      fetchTo
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.entityDetailsConfigForm = this.fb.group({
      detailsList: [configuration.detailsList, [Validators.required]],
      fetchTo: [configuration.fetchTo, []]
    });
  }
}
