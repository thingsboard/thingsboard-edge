import { Component, OnInit, Input, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup, FormControl } from '@angular/forms';
import { Observable } from 'rxjs';

import { mqttCredentialTypes } from '../../integartion-forms-templates';


@Component({
  selector: 'tb-mqtt-integration-form',
  templateUrl: './mqtt-integration-form.component.html',
  styleUrls: ['./mqtt-integration-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class MqttIntegrationFormComponent implements OnInit {


  @Input() form: FormGroup;
  @Input() topicFilters: FormGroup;
  @Input() downlinkTopicPattern: FormControl;

  mqttCredentialTypes = mqttCredentialTypes;

  constructor() { }

  ngOnInit(): void {
  }
}
