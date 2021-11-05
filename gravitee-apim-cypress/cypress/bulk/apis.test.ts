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

import { ApiFakers } from '../fakers/apis';
import { API_PUBLISHER_USER } from '../fakers/users/users';
import { Api } from '../model/apis';
import { gio } from '../commands/gravitee.commands';

const bulkSize = 50;

function createPublishAndStart() {
  gio
    .management(API_PUBLISHER_USER)
    .apis()
    .create(ApiFakers.api())
    .created()
    .should((createResponse) => {
      const apiId = createResponse.body.id;
      expect(apiId).not.undefined;
      expect(createResponse.body.state).to.eq('STOPPED');

      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .update(apiId, createResponse.body)
        .ok()
        .should((publishResponse) => {
          expect(publishResponse.body.lifecycle_state).to.eq('PUBLISHED');
          expect(publishResponse.body.visibility).to.eq('PUBLIC');
        });

      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .start(apiId)
        .noContent()
        .should((response) => {
          const result: Api = response.body;
          expect(result.state).to.equal('STARTED');
        });
    });
}

describe('Bulk APIs', () => {
  it(`should create, publish and start ${bulkSize} APIs`, () => {
    // Useful to run in parallel
    for (let i = 0; i < bulkSize; i++) {
      createPublishAndStart();
    }
  });
});
