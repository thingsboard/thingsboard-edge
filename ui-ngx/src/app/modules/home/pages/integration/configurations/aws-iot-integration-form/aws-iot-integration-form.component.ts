import { Component, OnInit, Input } from '@angular/core';
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { IntegrationType } from '@app/shared/models/integration.models';

@Component({
  selector: 'tb-aws-iot-integration-form',
  templateUrl: './aws-iot-integration-form.component.html',
  styleUrls: ['./aws-iot-integration-form.component.scss']
})
export class AwsIotIntegrationFormComponent implements OnInit {

  @Input() topicFilters: FormArray;
  @Input() downlinkTopicPattern: FormControl;
  @Input() form: FormGroup;
  @Input() integrationType: IntegrationType;

  constructor() { }

  ngOnInit(): void {
  }
}
