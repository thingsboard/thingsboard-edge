import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';

import { initialPositionInStream } from '../../integartion-forms-temapltes';

@Component({
  selector: 'tb-aws-kinesis-integration-form',
  templateUrl: './aws-kinesis-integration-form.component.html',
  styleUrls: ['./aws-kinesis-integration-form.component.scss']
})
export class AwsKinesisIntegrationFormComponent implements OnInit {

  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;
  @Input() form: FormGroup;


  initialPositionInStream  = initialPositionInStream;

  constructor() { }

  ngOnInit(): void {
  }


}
