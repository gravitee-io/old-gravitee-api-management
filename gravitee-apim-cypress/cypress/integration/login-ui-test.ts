import { urlSuffix } from "../fixtures/urls";

const { loginUrl } = urlSuffix;

describe("Feature Login", () => {
    before("Navigate to", () => {
        cy.visit(loginUrl);
    });
    it(`should launch the login page`, () => {
        cy.url().should("contain", "login");
    });
    it(`should have login page elements`, () => {
        cy.get(".title").should("be.visible");
        cy.get(".title").contains("Sign In");
    });
    it(`should be able to login`, () => {
        cy.get("#input_0").type("admin");
        cy.get("#input_1").type("admin");
        // cy.gvType("#input_0", "admin");
        // cy.gvType("#input_1", "admin");
        cy.get(".btn").click();
        cy.wait(30000);
    });
    xit(`should have new url after successful login`, () => {
        cy.url().should("contain", "environments");
    });
    xit(`should have homepage elements`, () => {
        cy.get(".gv-navbar-user-link").should("be.visible");
    });
});
