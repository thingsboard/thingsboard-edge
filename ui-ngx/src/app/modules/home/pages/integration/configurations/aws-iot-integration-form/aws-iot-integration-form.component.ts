import { Component, OnInit, Input } from '@angular/core';
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-aws-iot-integration-form',
  templateUrl: './aws-iot-integration-form.component.html',
  styleUrls: ['./aws-iot-integration-form.component.scss']
})
export class AwsIotIntegrationFormComponent implements OnInit {

  @Input() topicFilters: FormArray;
  @Input() downlinkTopicPattern: FormControl;
  @Input() form: FormGroup;

  constructor() { }

  ngOnInit(): void {
  }
}
