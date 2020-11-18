import {Component, OnInit} from '@angular/core';
import {PageComponent} from "@shared/components/page.component";
import {FormBuilder, FormGroup} from "@angular/forms";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {EdgeService} from "@core/http/edge.service";
import {CloudStatus} from "@shared/models/edge.models";
import {AttributeService} from "@core/http/attribute.service";
import {AttributeScope} from "@shared/models/telemetry/telemetry.models";
import {getCurrentAuthUser} from "@core/auth/auth.selectors";
import {EntityId} from "@shared/models/id/entity-id";
import {EntityType} from "@shared/models/entity-type.models";
import {DatePipe} from "@angular/common";

@Component({
  selector: 'tb-edge-info',
  templateUrl: './edge-info.component.html',
  styleUrls: ['./edge-info.component.scss']
})
export class EdgeInfoComponent extends PageComponent implements OnInit {

  edgeInfoGroup: FormGroup;
  cloudStatus: CloudStatus = {
    label: '',
    isActive: false
  }

  constructor(protected store: Store<AppState>,
              private edgeService: EdgeService,
              private attributeService: AttributeService,
              private datePipe: DatePipe,
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
      cloudType: '',
      lastConnectTime: '',
      lastDisconnectTime: ''
    });
    this.edgeInfoGroup.disable();
  }

  loadEdgeInfo() {
    const authUser = getCurrentAuthUser(this.store);
    const currentTenant: EntityId = {
      id: authUser.tenantId,
      entityType: EntityType.TENANT
    }
    this.attributeService.getEntityAttributes(currentTenant, AttributeScope.SERVER_SCOPE)
      .subscribe(attributes => {
        const edge: any = attributes.reduce(function (map, attribute) {
          map[attribute.key] = attribute;
          return map;
        }, {});
        const edgeSettings = JSON.parse(edge.edgeSettings.value);
        this.cloudStatus = {
          label: edge.active.value ? "edge.connected" : "edge.disconnected",
          isActive: edge.active.value
        }
        this.edgeInfoGroup.setValue({
          id: edgeSettings.edgeId,
          type: edgeSettings.type,
          routingKey: edgeSettings.routingKey,
          cloudType: edgeSettings.cloudType,
          lastConnectTime: this.datePipe.transform(edge.lastConnectTime.value, 'yyyy-MM-dd HH:mm:ss'),
          lastDisconnectTime: this.datePipe.transform(edge.lastDisconnectTime.value, 'yyyy-MM-dd HH:mm:ss')
        })
      })
  }

}
