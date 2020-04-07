import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-ttn-integration-form',
  templateUrl: './ttn-integration-form.component.html',
  styleUrls: ['./ttn-integration-form.component.scss']
})
export class TtnIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;

  hostTypes = {
    region: 'Region',
    custom: 'Custom'
  }


  constructor() { }

  ngOnInit(): void {
  }

  buildHostName() {/*
    scope.configuration.clientConfiguration.host = (scope.currentHostType === scope.hostTypes.region) ? (scope.hostRegion + hostRegionSuffix) : scope.hostCustom;
    scope.configuration.clientConfiguration.customHost = (scope.currentHostType === scope.hostTypes.custom);
  */}

}
