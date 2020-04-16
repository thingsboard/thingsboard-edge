import { Component, OnInit, Input, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-custom-integration-form',
  templateUrl: './custom-integration-form.component.html',
  styleUrls: ['./custom-integration-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class CustomIntegrationFormComponent implements OnInit {


  @Input() form: FormGroup;


  constructor() { }

  ngOnInit(): void {
  }

}
