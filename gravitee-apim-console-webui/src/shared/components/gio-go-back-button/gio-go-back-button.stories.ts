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
import { MatButtonModule } from '@angular/material/button';
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import { action } from '@storybook/addon-actions';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { GioGoBackComponent } from './gio-go-back-button.component';
import { GioGoBackButtonModule } from './gio-go-back-button.module';

import { UIRouterState } from '../../../ajs-upgraded-providers';

export default {
  title: 'Shared / Go back button',
  component: GioGoBackComponent,
  decorators: [
    moduleMetadata({
      imports: [BrowserAnimationsModule, MatButtonModule, GioGoBackButtonModule],
      providers: [{ provide: UIRouterState, useValue: { go: (...args) => action('Ajs state go')(args) } }],
    }),
  ],
  render: () => ({}),
} as Meta;

export const insideH1: Story = {
  render: () => ({
    template: `<h1><gio-go-back-button [ajsGo]="{ to: 'state', params: { id: '42' } }"></gio-go-back-button> Title 1</h1>`,
  }),
};
