import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';

import { mqttCredentialTypes } from '../../integartion-forms-templates';


@Component({
  selector: 'tb-mqtt-integration-form',
  templateUrl: './mqtt-integration-form.component.html',
  styleUrls: ['./mqtt-integration-form.component.scss']
})
export class MqttIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;
  @Input() topicFilters: FormGroup;
  @Input() downlinkTopicPattern: FormGroup;

  mqttCredentialTypes = mqttCredentialTypes;

  constructor() { }

  ngOnInit(): void {
  }

  credentialsTypeChanged = () => {
/*    var type = scope.configuration.clientConfiguration.credentials.type;
    scope.configuration.clientConfiguration.credentials = {};
    scope.configuration.clientConfiguration.credentials.type = type;
    scope.updateValidity();*/
}
}
