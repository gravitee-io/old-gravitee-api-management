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

import { ApplicationFakers } from '@fakers/applications';
import { Application } from '@model/applications';
import { User, BasicAuthentication } from '@model/users';

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Creates an application by sending a POST request to the API server
       *
       * User must have MANAGEMENT_APPLICATION[CREATE] permission to create an application.
       *
       * @param auth User authentication object
       * @example cy.createApi({username: "myApiUser", password: "password123"})
       */
      createApplication(auth: BasicAuthentication): Chainable;

      /**
       * Deletes an application by sending a DELETE request to the API server
       *
       * User must have the DELETE permission
       *
       * @param auth User authentication object
       * @param applicationId
       */
      deleteApplication(auth: BasicAuthentication, applicationId: string): Chainable;
    }
  }
}

Cypress.Commands.add('createApplication', (auth) => {
  const fakeApplication: Application = ApplicationFakers.application();

  return cy.request({
    method: 'POST',
    url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/applications`,
    body: fakeApplication,
    auth,
    failOnStatusCode: false,
  });
});

Cypress.Commands.add('deleteApplication', (auth, applicationId) => {
  return cy.request({
    method: 'DELETE',
    url: `${Cypress.config().baseUrl}${Cypress.env('managementApi')}/applications/${applicationId}`,
    auth,
    failOnStatusCode: false,
  });
});
