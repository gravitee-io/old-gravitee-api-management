describe("Feature API", () => {
    before(() => {
        cy.loginInAPIM();
    });

    it(`Visit Home board`, () => {
        cy.visit("http://localhost:3000");
        cy.wait(3000);
        cy.contains("Home board");
    });

    it(`Visit Search Apis`, () => {
        cy.visit("http://localhost:3000/#!/environments/DEFAULT/apis/");
        cy.wait(3000);
        cy.contains("incidunt");
    });
});
