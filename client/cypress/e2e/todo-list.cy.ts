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

  it('Should show 300 todos in list view', () => {
    page.getTodoListItems().should('have.length', 300);
  });

  it('Should type something in the owner filter and check that it returned correct elements', () => {
    // Filter for todo 'Roberta'
    cy.get('[data-test=todoOwnerInput]').type('Roberta');

    // All of the todo cards should have the owner we are filtering by
    page.getTodoListItems().each(e => {
      cy.wrap(e).find('.todo-list-owner').should('contain.text', 'Roberta');
    });
  });

  it('Should type something in the body filter and check that it returned correct elements', () => {
    // Filter for body 'aaaa'
    cy.get('[data-test=todoBodyInput]').type('a');

    page.getTodoListItems().should('have.lengthOf.above', 0);

    // All of the todo list items should have the body we are filtering by
    page.getTodoListItems().find('.todo-list-body').each(list => {
      cy.wrap(list).should('contain.text', 'a');
    });
  });

  it('Should type something partial in the body filter and check that it returned correct elements', () => {
    // Filter for companies that contain 'ti'
    cy.get('[data-test=todoBodyInput]').type('ti');

    // Each todo list items's body should include the text we are filtering by
    page.getTodoListItems().each(e => {
      cy.wrap(e).find('.todo-list-body').should('include.text', 'ti');
    });
  });

  it('Should type something in the category filter and check that it returned correct elements', () => {
    // Filter for todos of category 'homework'
    cy.get('[data-test=todoCategoryInput]').type('homework');

    // Each todo list items's category should include the text we are filtering by
    page.getTodoListItems().each(e => {
      cy.wrap(e).find('.todo-list-category').should('include.text', 'homework');
    });
  });

  it('Should select a complete status and check that it returned correct elements', () => {
    // Filter for status 'complete');
    page.selectStatus('complete');

    // Some of the todos should be listed
    page.getTodoListItems().should('have.lengthOf.above', 0);

    // All of the todo list items that show should have the status we are looking for
    page.getTodoListItems().each($e => {
      cy.wrap($e).find('.todo-list-status').as('status');
      cy.get('@status').find('.todo-list-status-complete').should('contain.text', 'Complete');
    });
  });

  it('Should select an incomplete status and check that it returned correct elements', () => {
    // Filter for status 'incomplete');
    page.selectStatus('incomplete');

    // Some of the todos should be listed
    page.getTodoListItems().should('have.lengthOf.above', 0);

    // All of the todo list items that show should have the status we are looking for
    page.getTodoListItems().each($e => {
      cy.wrap($e).find('.todo-list-status').as('status');
      cy.get('@status').find('.todo-list-status-incomplete').should('contain.text', 'Incomplete');
    });
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
