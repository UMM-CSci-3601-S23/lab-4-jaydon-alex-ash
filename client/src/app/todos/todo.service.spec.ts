import { HttpClient, HttpParams } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { of } from 'rxjs';
import { Todo } from './todo';
import { TodoService } from './todo.service';

describe('todoService', () => {
  // A small collection of test users
  const testTodos: Todo[] = [
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
  let todoService: TodoService;
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    todoService = new TodoService(httpClient);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('When getTodos() is called with no parameters', () => {
    it('calls `api/todos`', waitForAsync(() => {

      const mockedMethod = spyOn(httpClient, 'get').and.returnValue(of(testTodos));

      todoService.getTodos().subscribe((todos: Todo[]) => {
        // The array of `Todos`s returned by `getTodos()` should be
        // the array `testTodos`.
        expect(todos)
          .withContext('expected todos')
          .toEqual(testTodos);
        // The mocked method (`httpClient.get()`) should have been called
        // exactly one time.
        expect(mockedMethod)
          .withContext('one call')
          .toHaveBeenCalledTimes(1);
        // The mocked method should have been called with two arguments:
        //   * the appropriate URL ('/api/users' defined in the `UserService`)
        //   * An options object containing an empty `HttpParams`
        expect(mockedMethod)
          .withContext('talks to the correct endpoint')
          .toHaveBeenCalledWith(todoService.todoUrl, { params: new HttpParams() });
      });
    }));
  });

  describe('When getTodos() is called with parameters, it correctly forms the HTTP request (Javalin/Server filtering)', () => {
    /*
    * We really don't care what `getTodos()` returns in the cases
    * where the filtering is happening on the server. Since all the
    * filtering is happening on the server, `getTodos()` is really
    * just a "pass through" that returns whatever it receives, without
    * any "post processing" or manipulation. So the tests in this
    * `describe` block all confirm that the HTTP request is properly formed
    * and sent out in the world, but don't _really_ care about
    * what `getTodos()` returns as long as it's what the HTTP
    * request returns.
    *
    * So in each of these tests, we'll keep it simple and have
    * the (mocked) HTTP request return the entire list `testTodos`
    * even though in "real life" we would expect the server to
    * return return a filtered subset of the users.
    */

    it('correctly calls api/todos with filter parameter \'owner\'', () => {
        const mockedMethod = spyOn(httpClient, 'get').and.returnValue(of(testTodos));

        todoService.getTodos({ owner: 'roberta' }).subscribe((todos: Todo[]) => {
          // The array of `Todo`s returned by `getTodos()` should be
          // the array `testTodos`. This is "weird" because we'd truly be expecting
          // the server to return just `Roberta` but as mentioned above, we're
          // not trying to get the server here.
          expect(todos)
            .withContext('expected users')
            .toEqual(testTodos);
          expect(mockedMethod)
            .withContext('one call')
            .toHaveBeenCalledTimes(1);
          // The mocked method should have been called with two arguments:
          //   * the appropriate URL ('/api/todos' defined in the `TodoService`)
          //   * An options object containing an `HttpParams` with the `owner`:`roberta`
          //     key-value pair.
          expect(mockedMethod)
            .withContext('talks to the correct endpoint')
            .toHaveBeenCalledWith(todoService.todoUrl, { params: new HttpParams().set('owner', 'roberta') });
        });
    });

    it('correctly calls api/todos with filter parameter \'status\'', () => {
      const mockedMethod = spyOn(httpClient, 'get').and.returnValue(of(testTodos));

      todoService.getTodos({ status: 'complete' }).subscribe((todos: Todo[]) => {
        expect(todos)
          .withContext('expected todos')
          .toEqual(testTodos);
        expect(mockedMethod)
          .withContext('one call')
          .toHaveBeenCalledTimes(1);
        expect(mockedMethod)
          .withContext('talks to the correct endpoint')
          .toHaveBeenCalledWith(todoService.todoUrl, { params: new HttpParams().set('status', 'complete') });
      });
    });

    it('correctly calls api/todos with multiple filter parameters', () => {
        const mockedMethod = spyOn(httpClient, 'get').and.returnValue(of(testTodos));

        todoService.getTodos({ body: 'Hello', status: 'incomplete' }).subscribe((todos: Todo[]) => {
          // This test checks that the call to `todoService.getTodos()` does several things:
          //   * It returns the expected array of todos (namely `testTodos` as discussed above).
          //   * It calls the mocked method (`HttpClient#get()`) exactly once.
          //   * It calls it with the correct endpoint (`todoService.todoUrl`).
          //   * It calls it with the correct parameters:
          //      * There should be two parameters (this makes sure that there aren't extras).
          //      * There should be a "body:hello" key-value pair.
          //      * And a "status:incomplete" pair.

          // This gets the arguments for the first (and in this case only) call to the `mockMethod`.
          const [url, options] = mockedMethod.calls.argsFor(0);
          // Gets the `HttpParams` from the options part of the call.
          // `options.param` can return any of a broad number of types;
          // it is in fact an instance of `HttpParams`, and I need to use
          // that fact, so I'm casting it (the `as HttpParams` bit).
          const calledHttpParams: HttpParams = (options.params) as HttpParams;
          expect(todos)
            .withContext('expected todos')
            .toEqual(testTodos);
          expect(mockedMethod)
            .withContext('one call')
            .toHaveBeenCalledTimes(1);
          expect(url)
            .withContext('talks to the correct endpoint')
            .toEqual(todoService.todoUrl);
          expect(calledHttpParams.keys().length)
            .withContext('should have 2 params')
            .toEqual(2);
          expect(calledHttpParams.get('body'))
            .withContext('body of Hello')
            .toEqual('Hello');
          expect(calledHttpParams.get('status'))
            .withContext('status being incomplete')
            .toEqual('incomplete');
        });
    });
  });

  describe('When getTodoByID() is given an ID', () => {
    it('calls api/todos/id with the correct ID', waitForAsync(() => {
      // We're just picking a Todo "at random" from our little
      // set of Todos up at the top.
      const targetTodo: Todo = testTodos[1];
      const targetId: string = targetTodo._id;

      // Mock the `httpClient.get()` method so that instead of making an HTTP request
      // it just returns our test data
      const mockedMethod = spyOn(httpClient, 'get').and.returnValue(of(targetTodo));

      // Call `todoService.getTodo()` and confirm that the correct call has
      // been made with the correct arguments.
      //
      // We have to `subscribe()` to the `Observable` returned by `getTodoById()`.
      // The `todo` argument in the function below is the thing of type Todo returned by
      // the call to `getTodoById()`.
      todoService.getTodoById(targetId).subscribe((todo: Todo) => {
        // The `Todo` returned by `getTodoById()` should be targetTodo.
        // This `expect` doesn't do a _whole_ lot.
        // This really just confirms that `getTodoById()`
        // doesn't in some way modify the user it
        // gets back from the server.
        expect(todo)
          .withContext('expected todo')
          .toBe(targetTodo);
        expect(mockedMethod)
          .withContext('one call')
          .toHaveBeenCalledTimes(1);
        expect(mockedMethod)
          .withContext('talks to the correct endpoint')
          .toHaveBeenCalledWith(todoService.todoUrl + '/' + targetId);
      });
    }));
  });

  // We are filtering category, and body on the client.
  describe('Filtering on the client using `filterTodos()` (Angular/Client filtering)', () => {
    /*
     * Since `filterTodos` actually filters "locally" (in
     * Angular instead of on the server), we do want to
     * confirm that everything it returns has the desired
     * properties. Since this doesn't make a call to the server,
     * though, we don't have to use the mock HttpClient and
     * all those complications.
     */
    it('filters by category', () => {
      const todoCategory = 'i';
      const filteredTodos = todoService.filterTodos(testTodos, { category: todoCategory });
      // There should be one todo with an 'i' in its
      // category: 'video games'
      expect(filteredTodos.length).toBe(1);
      // Every returned todo's category should contain an 'i'.
      filteredTodos.forEach(todo => {
        expect(todo.category.indexOf(todoCategory)).toBeGreaterThanOrEqual(0);
      });
    });

    it('filters by body', () => {
      const todoBody = 'cool';
      const filteredTodos = todoService.filterTodos(testTodos, { body: todoBody });
      // There should be one todo with a 'cool' in its
      // body: 'im cool'
      expect(filteredTodos.length).toBe(1);
      // Every returned todo's category should contain a 'cool'.
      filteredTodos.forEach(todo => {
        expect(todo.body.indexOf(todoBody)).toBeGreaterThanOrEqual(0);
      });
    });
    });

  // We can't test for adding todos yet, so we will put this in later.
  /*describe('Adding a todo using `addTodo()`', () => {
    it('talks to the right endpoint and is called once', waitForAsync(() => {
      // Mock the `httpClient.addUser()` method, so that instead of making an HTTP request,
      // it just returns our test data.
      const TODO_ID = 'pat_id';
      const mockedMethod = spyOn(httpClient, 'post').and.returnValue(of(TODO_ID));

      // paying attention to what is returned (undefined) didn't work well here,
      // but I'm putting something in here to remind us to look into that
      todoService.addTodos(testTodos[1]).subscribe((returnedString) => {
        console.log('The thing returned was:' + returnedString);
        expect(mockedMethod)
          .withContext('one call')
          .toHaveBeenCalledTimes(1);
        expect(mockedMethod)
          .withContext('talks to the correct endpoint')
          .toHaveBeenCalledWith(todoService.todoUrl, testTodos[1]);
      });
    }));
  });*/
});
