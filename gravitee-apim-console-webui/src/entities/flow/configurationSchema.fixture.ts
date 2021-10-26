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
import { FlowConfigurationSchema } from './configurationSchema';

export function fakeFlowConfigurationSchema(attributes?: FlowConfigurationSchema): FlowConfigurationSchema {
  const base: FlowConfigurationSchema = {
    type: 'object',
    id: 'apim',
    properties: {
      'flow-mode': {
        title: 'Flow Mode',
        description: 'The flow mode',
        type: 'string',
        enum: ['DEFAULT', 'BEST_MATCH'],
        default: 'DEFAULT',
        'x-schema-form': {
          titleMap: {
            DEFAULT: 'Default',
            BEST_MATCH: 'Best match',
          },
        },
      },
    },
    required: [],
    disabled: [],
  };

  return { ...base, ...attributes };
}
