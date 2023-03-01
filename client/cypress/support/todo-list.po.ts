export class TodoListPage {
  navigateTo() {
    return cy.visit('/todos');
  }

  /**
   * Gets the title of the app when visiting the `/todos` page.
   *
   * @returns the value of the element with the ID `.todo-list-title`
   */
  getTodoTitle() {
    return cy.get('.todo-list-title');
  }

  /**
   * Get all the `app-todo-card` DOM elements. This will be
   * empty if we're using the list view of the todos.
   *
   * @returns an iterable (`Cypress.Chainable`) containing all
   *   the `app-todo-card` DOM elements.
   */
   getTodoCards() {
    return cy.get('.todo-cards-container app-todo-card');
  }

  /**
   * Get all the `.todo-list-item` DOM elements. This will
   * be empty if we're using the card view of the todos.
   *
   * @returns an iterable (`Cypress.Chainable`) containing all
   *   the `.todo-list-item` DOM elements.
   */
  getTodoListItems() {
    return cy.get('.todo-nav-list .todo-list-item');
  }

  /**
   * Selects a status to filter in the "Status" selector.
   *
   * @param value The role *value* to select, this is what's found in the mat-option "value" attribute.
   */
  selectStatus(value: string) {
    // Find and click the drop down
    cy.get('[data-test=todoStatusSelect]').click();
    // Select and click the desired value from the resulting menu
    return cy.get(`mat-option[value="${value}"]`).click();
  }

  addTodoButton() {
    return cy.get('[data-test=addTodoButton]');
  }
}
