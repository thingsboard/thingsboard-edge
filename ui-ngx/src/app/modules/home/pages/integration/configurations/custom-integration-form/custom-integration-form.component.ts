import { Component, OnInit, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-custom-integration-form',
  templateUrl: './custom-integration-form.component.html',
  styleUrls: ['./custom-integration-form.component.scss']
})
export class CustomIntegrationFormComponent implements OnInit {

  
  @Input() form: FormGroup;


  constructor() { }

  ngOnInit(): void {
  }

}
