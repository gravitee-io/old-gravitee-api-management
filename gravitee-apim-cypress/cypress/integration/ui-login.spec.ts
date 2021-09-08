import { urlSuffix } from "../fixtures/urls";

const { loginUrl } = urlSuffix;

describe("Feature Login", () => {
    // Here we use a beforeEach and an after each to reset properly all the cookie we want
    // otherwise we are sometimes redirected or face XRCF issues
    beforeEach(() => {
        cy.clearCookie("Auth-Graviteeio-APIM");
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

        cy.get(".btn").click();
        cy.contains("Home board");
    });
});
