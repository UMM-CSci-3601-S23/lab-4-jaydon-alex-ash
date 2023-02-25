import { TodoListPage } from '../support/todo-list.po';

const page = new TodoListPage();

describe('Todo list', () => {

  before(() => {
    cy.task('seed:database');
  });

  beforeEach(() => {
    page.navigateTo();
  });

  it('Should have the correct title', () => {
    page.getTodoTitle().should('have.text', 'Todos');
  });

  it('Should show 10 todos in list view', () => {
    page.getTodoListItems().should('have.length', 10);
  });

  it('Should type something in the owner filter and check that it returned correct elements', () => {
    // Filter for todo 'Roberta'
    cy.get('[data-test=todoOwnerInput]').type('Roberta');

    /*// All of the todo cards should have the name we are filtering by
    page.getTodoCards().each(e => {
      cy.wrap(e).find('.todo-card-name').should('have.text', 'Lynn Ferguson');
    });

    // (We check this two ways to show multiple ways to check this)
    page.getTodoCards().find('.todo-card-name').each(el =>
      expect(el.text()).to.equal('Lynn Ferguson')
    );*/
  });

  it('Should type something in the body filter and check that it returned correct elements', () => {
    // Filter for body 'aaaa'
    cy.get('[data-test=todoBodyInput]').type('aaaa');

    page.getTodoListItems().should('have.lengthOf.above', 0);

    /*// All of the todo cards should have the body we are filtering by
    page.getTodoCards().find('.todo-card-company').each(card => {
      cy.wrap(card).should('have.text', 'OHMNET');
    });*/
  });

  it('Should type something partial in the body filter and check that it returned correct elements', () => {
    // Filter for companies that contain 'ti'
    cy.get('[data-test=todoBodyInput]').type('ti');

    /*page.getTodoListItems().should('have.lengthOf', 2);

    // Each todo card's company name should include the text we are filtering by
    page.getTodoCards().each(e => {
      cy.wrap(e).find('.todo-card-company').should('include.text', 'TI');
    });*/
  });

  it('Should type something in the category filter and check that it returned correct elements', () => {
    // Filter for todos of category 'homework'
    cy.get('[data-test=todoCategoryInput]').type('homework');

    /*page.getTodoCards().should('have.lengthOf', 3);

    // Go through each of the cards that are being shown and get the names
    page.getTodoCards().find('.todo-card-name')
      // We should see these todos whose age is 27
      .should('contain.text', 'Stokes Clayton')
      .should('contain.text', 'Bolton Monroe')
      .should('contain.text', 'Merrill Parker')
      // We shouldn't see these todos
      .should('not.contain.text', 'Connie Stewart')
      .should('not.contain.text', 'Lynn Ferguson');*/
  });

  it('Should select a status and check that it returned correct elements', () => {
    // Filter for status 'complete');
    page.selectStatus('complete');

    /*// Some of the todos should be listed
    page.getTodoListItems().should('have.lengthOf.above', 0);

    // All of the todo list items that show should have the role we are looking for
    page.getTodoListItems().each(el => {
      cy.wrap(el).find('.todo-list-role').should('contain', 'viewer');
    });*/
  });

  /*it('Should click add todo and go to the right URL', () => {
    // Click on the button for adding a new todo
    page.addTodoButton().click();

    // The URL should end with '/todos/new'
    cy.url().should(url => expect(url.endsWith('/todos/new')).to.be.true);

    // On the page we were sent to, We should see the right title
    cy.get('.add-todo-title').should('have.text', 'New Todo');
  });*/

});
