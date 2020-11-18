import { Component, OnInit } from '@angular/core';
import {PageComponent} from "@shared/components/page.component";
import {FormBuilder, FormGroup} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {EdgeService} from "@core/http/edge.service";
import {EdgeSettings} from "@shared/models/edge.models";
import {AttributeService} from "@core/http/attribute.service";
import {AttributeScope} from "@shared/models/telemetry/telemetry.models";

@Component({
  selector: 'tb-edge-info',
  templateUrl: './edge-info.component.html',
  styleUrls: ['./edge-info.component.scss']
})
export class EdgeInfoComponent extends PageComponent implements OnInit {

  edgeInfoGroup: FormGroup;
  edgeSettings: EdgeSettings;
  attributes: any;

  constructor(protected store: Store<AppState>,
              private edgeService: EdgeService,
              private attributeService: AttributeService,
              public fb: FormBuilder) {
    super(store);
    this.buildEdgeInfoForm();
  }

  ngOnInit(): void {
    this.loadEdgeInfo();
  }

  buildEdgeInfoForm() {
    this.edgeInfoGroup = this.fb.group({
      id: '',
      type: '',
      routingKey: '',
      cloudType: ''
    });
    this.edgeInfoGroup.disable();
  }

  loadEdgeInfo() {
    this.edgeService.getEdgeSettings()
      .subscribe(edgeSettings => {
        this.loadEdgeAttributes(edgeSettings.edgeId);
        this.edgeInfoGroup.setValue({
          id: edgeSettings.edgeId,
          type: edgeSettings.type,
          routingKey: edgeSettings.routingKey,
          cloudType: edgeSettings.cloudType
        })
      }
      );
  }

  loadEdgeAttributes(edgeId) {
    this.attributeService.getEdgeAttributes(edgeId, AttributeScope.SERVER_SCOPE)
      .subscribe(
        attributes => {
          this.attributes = attributes;
        }
      );
  }


  // onUpdate(attributes) {
  //   this.queueStartTs = 0;
  //   let edge = attributes.reduce(function (map, attribute) {
  //     map[attribute.key] = attribute;
  //     return map;
  //   }, {});
  //   if (edge.queueStartTs) {
  //     this.queueStartTs = edge.queueStartTs.lastUpdateTs;
  //   }
  // }

}
