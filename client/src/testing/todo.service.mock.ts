import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Todo } from '../app/todos/todo';
import { TodoService } from '../app/todos/todo.service';

/**
 * A "mock" version of the `TodoService` that can be used to test components
 * without having to create an actual service. It needs to be `Injectable` since
 * that's how services are typically provided to components.
 */
@Injectable()
export class MockTodoService extends TodoService {
  static testTodos: Todo[] = [
    {
        _id: 'chris_id',
        owner: 'chris',
        status: false,
        body: 'Hello',
        category: 'Homework'
      },
      {
        _id: 'sam_id',
        owner: 'sam',
        status: false,
        body: 'im cool',
        category: 'Video Games'
      },
      {
        _id: 'sofia_id',
        owner: 'sofia',
        status: true,
        body: 'Maybe hello or bye',
        category: 'Development'
      }
  ];

  constructor() {
    super(null);
  }

  getTodos(filters?: { owner?: string; body?: string; status?: string; limit?: number; sort?: string }): Observable<Todo[]> {
    // Our goal here isn't to test (and thus rewrite) the service, so we'll
    // keep it simple and just return the test users regardless of what
    // filters are passed in.
    //
    // The `of()` function converts a regular object or value into an
    // `Observable` of that object or value.
    return of(MockTodoService.testTodos);
  }
}
