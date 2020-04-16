import { Component, OnInit, Input, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-aws-sqs-integration-form',
  templateUrl: './aws-sqs-integration-form.component.html',
  styleUrls: ['./aws-sqs-integration-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class AwsSqsIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;

  constructor() { }

  ngOnInit(): void {
  }

}
