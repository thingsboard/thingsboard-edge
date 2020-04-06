import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';
import { mqttCredentialTypes } from '../../integartionFormTemapltes';


//mqttCredentialTypes

@Component({
  selector: 'tb-mqtt-integration-form',
  templateUrl: './mqtt-integration-form.component.html',
  styleUrls: ['./mqtt-integration-form.component.scss']
})
export class MqttIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;
  integrationTypes = IntegrationType;
  mqttCredentialTypes = mqttCredentialTypes;

  constructor() { }

  ngOnInit(): void {
  }

}
