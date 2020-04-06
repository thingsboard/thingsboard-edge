import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';

@Component({
  selector: 'tb-ibm-watson-iot-integration-form',
  templateUrl: './ibm-watson-iot-integration-form.component.html',
  styleUrls: ['./ibm-watson-iot-integration-form.component.scss']
})
export class IbmWatsonIotIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  constructor() { }

  ngOnInit(): void {
  }

}
