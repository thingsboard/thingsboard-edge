// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EdgeService } from '@core/http/edge.service';
import { EdgeSettings } from '@shared/models/edge.models';
import { AttributeService } from '@core/http/attribute.service';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-edge-status',
  templateUrl: './edge-status.component.html',
  styleUrls: ['./edge-status.component.scss'],
  standalone: false
})
export class EdgeStatusComponent extends PageComponent implements OnInit {

  isActive: boolean;
  edgeStatusGroup: FormGroup;

  constructor(public fb: FormBuilder,
              protected store: Store<AppState>,
              private edgeService: EdgeService,
              private attributeService: AttributeService,
              private datePipe: DatePipe,
              private route: ActivatedRoute) {
    super(store);
    this.buildForm();
  }

  ngOnInit(): void {
    this.init();
  }

  buildForm() {
    this.edgeStatusGroup = this.fb.group({
      name: '',
      id: '',
      type: '',
      routingKey: '',
      lastConnectTime: '',
      lastDisconnectTime: ''
    });
    this.edgeStatusGroup.disable();
  }

  private init(): void {
    this.route.data.subscribe(data => {
        const edgeAttributes = data.edgeAttributes.reduce(function (map, attribute) {
          map[attribute.key] = attribute;
          return map;
        }, {});

        const isActive: boolean = edgeAttributes.active.value;
        const lastConnectTime: number = edgeAttributes.lastConnectTime.value;
        const lastDisconnectTime: number = edgeAttributes.lastDisconnectTime?.value;
        const edgeSettings: EdgeSettings = JSON.parse(edgeAttributes.edgeSettings.value);

        this.isActive = isActive;
        this.edgeStatusGroup.setValue({
          name: edgeSettings.name,
          id: edgeSettings.edgeId,
          type: edgeSettings.type,
          routingKey: edgeSettings.routingKey,
          lastConnectTime: this.datePipe.transform(lastConnectTime, 'yyyy-MM-dd HH:mm:ss'),
          lastDisconnectTime: lastDisconnectTime ? this.datePipe.transform(lastDisconnectTime, 'yyyy-MM-dd HH:mm:ss') : 'N/A'
        });
      }
    );
  }

}
