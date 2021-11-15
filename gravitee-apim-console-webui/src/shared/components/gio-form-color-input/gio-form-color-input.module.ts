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
import { A11yModule } from '@angular/cdk/a11y';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatRippleModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';

import { GioFormColorInputComponent } from './gio-form-color-input.component';

@NgModule({
  imports: [CommonModule, A11yModule, MatInputModule, ReactiveFormsModule, MatRippleModule],
  declarations: [GioFormColorInputComponent],
  exports: [GioFormColorInputComponent],
  entryComponents: [GioFormColorInputComponent],
})
export class GioFormColorInputModule {}
