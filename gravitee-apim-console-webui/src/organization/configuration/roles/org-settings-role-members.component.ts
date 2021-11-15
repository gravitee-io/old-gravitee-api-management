/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';

@Component({
  selector: 'org-settings-role-members',
  template: require('./org-settings-role-members.component.html'),
  styles: [require('./org-settings-role-members.component.scss')],
})
export class OrgSettingsRoleMembersComponent implements OnInit, OnDestroy {
  private readonly unsubscribe$ = new Subject<boolean>();

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  ngOnInit(): void {}

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
