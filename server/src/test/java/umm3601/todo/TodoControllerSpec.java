package umm3601.todo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
//import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

//import io.javalin.validation.BodyValidator;
import io.javalin.validation.ValidationException;
import io.javalin.validation.Validator;
//import umm3601.todo.Todo;
//import umm3601.todo.TodoController;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
//import io.javalin.json.JavalinJackson;

/**
 * Tests the logic of the TodoController
 *
 * @throws IOException
 */
// The tests here include a ton of "magic numbers" (numeric constants).
// It wasn't clear to me that giving all of them names would actually
// help things. The fact that it wasn't obvious what to call some
// of them says a lot. Maybe what this ultimately means is that
// these tests can/should be restructured so the constants (there are
// also a lot of "magic strings" that Checkstyle doesn't actually
// flag as a problem) make more sense.
@SuppressWarnings({ "MagicNumber" })
public class TodoControllerSpec {

  // An instance of the controller we're testing that is prepared in
  // `setupEach()`, and then exercised in the various tests below.
  private TodoController todoController;

  // The client and database that will be used
  // for all the tests in this spec file.
  private static MongoClient mongoClient;
  private static MongoDatabase db;

  // Used to translate between JSON and POJOs.
  //private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  // A Mongo object ID that is initialized in `setupEach()` and used
  // in a few of the tests. It isn't used all that often, though,
  // which suggests that maybe we should extract the tests that
  // care about it into their own spec file?
  private ObjectId samsId;

  @Captor
  private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;

  @Captor
  private ArgumentCaptor<Todo> todoCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  /**
   * Sets up (the connection to the) DB once; that connection and DB will
   * then be (re)used for all the tests, and closed in the `teardown()`
   * method. It's somewhat expensive to establish a connection to the
   * database, and there are usually limits to how many connections
   * a database will support at once. Limiting ourselves to a single
   * connection that will be shared across all the tests in this spec
   * file helps both speed things up and reduce the load on the DB
   * engine.
   */
  @BeforeAll
  public static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build()
    );
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  public static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  public void setupEach() throws IOException {
    // Reset our mock context and argument captor (declared with Mockito annotations @Mock and @Captor)
    MockitoAnnotations.openMocks(this);

    // Setup database
    MongoCollection<Document> todoDocuments = db.getCollection("todos");
    todoDocuments.drop();
    List<Document> testTodos = new ArrayList<>();
    testTodos.add(
        new Document()
            .append("owner", "Fry")
            .append("status", true)
            .append("body", "Lorem ipsum")
            .append("category", "video games"));
    testTodos.add(
        new Document()
            .append("owner", "Egg")
            .append("status", true)
            .append("body", "Bob Har")
            .append("category", "Food"));
    testTodos.add(
        new Document()
            .append("owner", "Watson")
            .append("status", false)
            .append("body", "Walter")
            .append("category", "dog toys"));
    testTodos.add(
        new Document()
            .append("owner", "Egg")
            .append("status", false)
            .append("body", "Aaaaaaaaa")
            .append("category", "cool gaming"));

    // A Mongo object ID that is initialized in `setupEach()` and used
    // in a few of the tests. It isn't used all that often, though,
    // which suggests that maybe we should extract the tests that
    // care about it into their own spec file?
    //ObjectId samsId = new ObjectId();
    samsId = new ObjectId();
    Document sam = new Document()
            .append("_id", samsId)
            .append("owner", "Sam")
            .append("status", true)
            .append("body", "Lorem ipsum")
            .append("category", "video games");

    todoDocuments.insertMany(testTodos);
    todoDocuments.insertOne(sam);

    todoController = new TodoController(db);
  }

  @Test
  public void canGetAllTodos() throws IOException {
    // When something asks the (mocked) context for the queryParamMap,
    // it will return an empty map (since there are no query params in this case where we want all todos)
    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());

    // Now, go ahead and ask the todoController to getTodos
    // (which will, indeed, ask the context for its queryParamMap)
    todoController.getTodos(ctx);

    // We are going to capture an argument to a function, and the type of that argument will be
    // of type ArrayList<Todo> (we said so earlier using a Mockito annotation like this):
    // @Captor
    // private ArgumentCaptor<ArrayList<Todo>> todoArrayListCaptor;
    // We only want to declare that captor once and let the annotation
    // help us accomplish reassignment of the value for the captor
    // We reset the values of our annotated declarations using the command
    // `MockitoAnnotations.openMocks(this);` in our @BeforeEach

    // Specifically, we want to pay attention to the ArrayList<Todo> that is passed as input
    // when ctx.json is called --- what is the argument that was passed? We capture it and can refer to it later
    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Check that the database collection holds the same number of documents as the size of the captured List<Todo>
    assertEquals(db.getCollection("todos").countDocuments(), todoArrayListCaptor.getValue().size());
  }

  @Test
  public void canGetTodosWithOwner() throws IOException {
    // Add a query param map to the context that maps "owner" to "fry".
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {"fry"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParamAsClass(TodoController.OWNER_KEY, String.class))
      .thenReturn(Validator.create(String.class, "fry", TodoController.OWNER_KEY));

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(1, todoArrayListCaptor.getValue().size());
  }

  /**
   * Test that if the todo sends a request with an illegal value in
   * the age field (i.e., something that can't be parsed to a number)
   * we get a reasonable error code back.
   */
  @Test
  public void respondsAppropriatelyToEmptyOwner() {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {""}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    when(ctx.queryParamAsClass(TodoController.OWNER_KEY, String.class))
      .thenReturn(Validator.create(String.class, "", TodoController.OWNER_KEY));

    // This should now throw a `ValidationException` because
    // our request has an age that can't be parsed to a number,
    // but I don't yet know how to make the message be anything specific
    assertThrows(ValidationException.class, () -> {
      todoController.getTodos(ctx);
    });
  }

  @Test
  public void getTodosByOwner() throws JsonMappingException, JsonProcessingException {
    // When the controller calls `ctx.queryParamMap`, return the expected map for an
    // "?owner=fry" query.
    when(ctx.queryParamMap()).thenReturn(Map.of(TodoController.OWNER_KEY, List.of("fry")));
    // When the controller calls `ctx.queryParamAsClass() to get the value associated with
    // the "owner" key, return an appropriate Validator. TBH, I never did figure out what the
    // third argument to the Validator constructor was for, but `null` seems OK. I'm also not sure
    // what the first argument is; it appears that you can set it to anything that isn't
    // null and it's happy.
    Validator<String> validator = new Validator<String>("owner", "fry", null);
    when(ctx.queryParamAsClass(TodoController.OWNER_KEY, String.class)).thenReturn(validator);

    // Call the method under test.
    todoController.getTodos(ctx);

    // Verify that `getTodos` called `ctx.status(200)` at some point.
    verify(ctx).status(HttpStatus.OK);

    // Verify that `ctx.json()` is called with a `List` of `Todo`s.
    // Each of those `Todo`s should have age 37.
    verify(ctx).json(argThat(new ArgumentMatcher<List<Todo>>() {
      public boolean matches(List<Todo> todos) {
        for (Todo todo : todos) {
          assertEquals("Fry", todo.owner);
        }
        return true;
      }
    }));
  }

  @Test
  public void canGetTodosWithStatusComplete() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(new String[] {"complete"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    Validator<String> validator = new Validator<String>("status", "complete", null);
    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, String.class)).thenReturn(validator);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Confirm that all the todos passed to `json` are complete.
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertEquals(true, todo.status);
    }
  }

  @Test
  public void canGetTodosWithStatusIncomplete() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(new String[] {"incomplete"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    Validator<String> validator = new Validator<String>("status", "incomplete", null);
    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, String.class)).thenReturn(validator);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    // Confirm that all the todos passed to `json` are incomplete.
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertEquals(false, todo.status);
    }
  }

  @Test
  public void respondsAppropriatelyToInvalidStatus() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(new String[] {"bad"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    Validator<String> validator = new Validator<String>("status", "bad", null);
    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, String.class)).thenReturn(validator);

    assertThrows(ValidationException.class, () -> {
      todoController.getTodos(ctx);
    });
  }

  @Test
  public void getTodosByOwnerAndStatus() throws IOException {
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(TodoController.OWNER_KEY, Arrays.asList(new String[] {"Egg"}));
    queryParams.put(TodoController.STATUS_KEY, Arrays.asList(new String[] {"incomplete"}));
    when(ctx.queryParamMap()).thenReturn(queryParams);
    Validator<String> ownerValidator = new Validator<String>("owner", "egg", null);
    when(ctx.queryParamAsClass(TodoController.OWNER_KEY, String.class)).thenReturn(ownerValidator);
    Validator<String> statusValidator = new Validator<String>("status", "incomplete", null);
    when(ctx.queryParamAsClass(TodoController.STATUS_KEY, String.class)).thenReturn(statusValidator);

    todoController.getTodos(ctx);

    verify(ctx).json(todoArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(1, todoArrayListCaptor.getValue().size());
    for (Todo todo : todoArrayListCaptor.getValue()) {
      assertEquals("Egg", todo.owner);
      assertEquals(false, todo.status);
    }
  }

  @Test
  public void getTodoWithExistentId() throws IOException {
    String id = samsId.toHexString();
    when(ctx.pathParam("id")).thenReturn(id);

    todoController.getTodo(ctx);

    verify(ctx).json(todoCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals("Sam", todoCaptor.getValue().owner);
    assertEquals(samsId.toHexString(), todoCaptor.getValue()._id);
  }

  @Test
  public void getTodoWithBadId() throws IOException {
    when(ctx.pathParam("id")).thenReturn("bad");

    Throwable exception = assertThrows(BadRequestResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo id wasn't a legal Mongo Object ID.", exception.getMessage());
  }

  @Test
  public void getTodoWithNonexistentId() throws IOException {
    String id = "588935f5c668650dc77df581";
    when(ctx.pathParam("id")).thenReturn(id);

    Throwable exception = assertThrows(NotFoundResponse.class, () -> {
      todoController.getTodo(ctx);
    });

    assertEquals("The requested todo was not found", exception.getMessage());
  }

  /*@Test
  public void addTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"Test Todo\","
        + "\"age\": 25,"
        + "\"company\": \"testers\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    todoController.addNewTodo(ctx);
    verify(ctx).json(mapCaptor.capture());

    // Our status should be 201, i.e., our new todo was successfully created.
    verify(ctx).status(HttpStatus.CREATED);

    //Verify that the todo was added to the database with the correct ID
    Document addedTodo = db.getCollection("todos")
      .find(eq("_id", new ObjectId(mapCaptor.getValue().get("id")))).first();

    // Successfully adding the todo should return the newly generated, non-empty MongoDB ID for that todo.
    assertNotEquals("", addedTodo.get("_id"));
    assertEquals("Test Todo", addedTodo.get("name"));
    assertEquals(25, addedTodo.get(TodoController.AGE_KEY));
    assertEquals("testers", addedTodo.get(TodoController.COMPANY_KEY));
    assertEquals("test@example.com", addedTodo.get("email"));
    assertEquals("viewer", addedTodo.get(TodoController.ROLE_KEY));
    assertNotNull(addedTodo.get("avatar"));
  }

  @Test
  public void addInvalidEmailTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"Test Todo\","
        + "\"age\": 25,"
        + "\"company\": \"testers\","
        + "\"email\": \"invalidemail\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });

    // Our status should be 400, because our request contained information that didn't validate.
    // However, I'm not yet sure how to test the specifics about validation problems encountered.
    // verify(ctx).status(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void addInvalidAgeTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"Test Todo\","
        + "\"age\": \"notanumber\","
        + "\"company\": \"testers\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void add0AgeTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"Test Todo\","
        + "\"age\": 0,"
        + "\"company\": \"testers\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void add150AgeTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"Test Todo\","
        + "\"age\": 150,"
        + "\"company\": \"testers\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void addNullNameTodo() throws IOException {
    String testNewTodo = "{"
        + "\"age\": 25,"
        + "\"company\": \"testers\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void addInvalidNameTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"\","
        + "\"age\": 25,"
        + "\"company\": \"testers\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void addInvalidRoleTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"Test Todo\","
        + "\"age\": 25,"
        + "\"company\": \"testers\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"invalidrole\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void addNullCompanyTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"Test Todo\","
        + "\"age\": 25,"
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void addInvalidCompanyTodo() throws IOException {
    String testNewTodo = "{"
        + "\"name\": \"\","
        + "\"age\": 25,"
        + "\"company\": \"\","
        + "\"email\": \"test@example.com\","
        + "\"role\": \"viewer\""
        + "}";
    when(ctx.bodyValidator(Todo.class))
      .then(value -> new BodyValidator<Todo>(testNewTodo, Todo.class, javalinJackson));

    assertThrows(ValidationException.class, () -> {
      todoController.addNewTodo(ctx);
    });
  }

  @Test
  public void deleteFoundTodo() throws IOException {
    String testID = samsId.toHexString();
    when(ctx.pathParam("id")).thenReturn(testID);

    // Todo exists before deletion
    assertEquals(1, db.getCollection("todos").countDocuments(eq("_id", new ObjectId(testID))));

    todoController.deleteTodo(ctx);

    verify(ctx).status(HttpStatus.OK);

    // Todo is no longer in the database
    assertEquals(0, db.getCollection("todos").countDocuments(eq("_id", new ObjectId(testID))));
  }

  @Test
  public void tryToDeleteNotFoundTodo() throws IOException {
    String testID = samsId.toHexString();
    when(ctx.pathParam("id")).thenReturn(testID);

    todoController.deleteTodo(ctx);
    // Todo is no longer in the database
    assertEquals(0, db.getCollection("todos").countDocuments(eq("_id", new ObjectId(testID))));

    assertThrows(NotFoundResponse.class, () -> {
      todoController.deleteTodo(ctx);
    });

    verify(ctx).status(HttpStatus.NOT_FOUND);

    // Todo is still not in the database
    assertEquals(0, db.getCollection("todos").countDocuments(eq("_id", new ObjectId(testID))));
  }*/

}
