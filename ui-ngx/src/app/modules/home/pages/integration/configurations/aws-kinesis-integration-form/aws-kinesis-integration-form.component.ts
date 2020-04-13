import { Component, Input, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { initialPositionInStream } from '../../integration-forms-templates';

@Component({
  selector: 'tb-aws-kinesis-integration-form',
  templateUrl: './aws-kinesis-integration-form.component.html',
  styleUrls: ['./aws-kinesis-integration-form.component.scss']
})
export class AwsKinesisIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;


  initialPositionInStream  = initialPositionInStream;

  constructor() { }

  ngOnInit(): void {
  }


}
