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
// import { createApi, publishApi, deleteApi } from "../../commands/management/api-management-commands";
import { ADMIN_USER, API_PUBLISHER_USER, LOW_PERMISSION_USER } from '@fakers/users/users';

describe('Testing search function in the UI', () => {
  beforeEach(() => {
    cy.createApi(ADMIN_USER).should((response) => {
      expect(response.status).to.equal(201);
      cy.wrap(response.body).as('createdApi');
    });
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit(`${Cypress.env('managementUI')}/#!/environments/DEFAULT/apis/`);
  });

  // WIP: receives an 403 status code
  // afterEach(function () {
  //     cy.deleteApi(ADMIN_USER, this.createdApi.id).should((response) => {
  //         expect(response.status).to.be.equal(204);
  //     })
  // })

  it('should find newly created API using its API ID in search', function () {
    cy.get('input').type(`${this.createdApi.id}{enter}`);
    cy.get('.md-body > .md-row').should('have.lengthOf', 1);
    cy.contains(this.createdApi.name).should('have.lengthOf', 1);
  });

  it('should find newly created API using its API name in search', function () {
    cy.get('input').type(`name: "${this.createdApi.name}"{enter}`);
    cy.contains(this.createdApi.name).should('have.lengthOf', 1);
    cy.get('.md-body > .md-row').should('have.lengthOf', 1);
  });
});
