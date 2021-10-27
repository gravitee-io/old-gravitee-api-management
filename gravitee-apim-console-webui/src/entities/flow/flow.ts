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
export interface PathOperator {
  path: string;
  operator: 'STARTS_WITH' | 'EQUALS';
}

export interface Step {
  name: string;
  policy: string;
  description: string;
  configuration: unknown;
  enabled: boolean;
}

export interface Consumer {
  consumerType: 'TAG';
  consumerId: string;
}

export interface Flow {
  name: string;
  'path-operator': PathOperator;
  pre: Step[];
  post: Step[];
  enabled: boolean;
  methods: string[];
  condition: string;
  consumers: Consumer[];
}
