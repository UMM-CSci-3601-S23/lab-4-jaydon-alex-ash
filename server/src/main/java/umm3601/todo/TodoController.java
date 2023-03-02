package umm3601.todo;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.regex.Pattern;

//import javax.lang.model.util.ElementScanner14;

import com.mongodb.client.MongoDatabase;
/*import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;*/

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

/**
 * Controller that manages requests for info about todos.
 */
public class TodoController {

  static final String OWNER_KEY = "owner";
  static final String STATUS_KEY = "status";

  private final JacksonMongoCollection<Todo> todoCollection;

  /**
   * Construct a controller for todos.
   *
   * @param database the database containing todo data
   */
  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
        database,
        "todos",
        Todo.class,
        UuidRepresentation.STANDARD);
  }

  /**
   * Set the JSON body of the response to be the single todo
   * specified by the `id` parameter in the request
   *
   * @param ctx a Javalin HTTP context
   */
  public void getTodo(Context ctx) {
    String id = ctx.pathParam("id");
    Todo todo;

    try {
      todo = todoCollection.find(eq("_id", new ObjectId(id))).first();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested todo id wasn't a legal Mongo Object ID.");
    }
    if (todo == null) {
      throw new NotFoundResponse("The requested todo was not found");
    } else {
      ctx.json(todo);
      ctx.status(HttpStatus.OK);
    }
  }

  /**
   * Set the JSON body of the response to be a list of all the todos returned from the database
   * that match any requested filters and ordering
   *
   * @param ctx a Javalin HTTP context
   */
  public void getTodos(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    Bson sortingOrder = constructSortingOrder(ctx);

    // All three of the find, sort, and into steps happen "in parallel" inside the
    // database system. So MongoDB is going to find the todos with the specified
    // properties, return those sorted in the specified manner, and put the
    // results into an initially empty ArrayList.
    ArrayList<Todo> matchingTodos = todoCollection
      .find(combinedFilter)
      .sort(sortingOrder)
      // .into seems to be telling Mongo how it will format the data returned (as an
      // ArrayList<>)
      .into(new ArrayList<>());

    // Set the JSON body of the response to be the list of todos returned by the database.
    // According to the Javalin documentation (https://javalin.io/documentation#context),
    // this calls result(jsonString), and also sets content type to json
    ctx.json(matchingTodos);

    // Explicitly set the context status to OK
    ctx.status(HttpStatus.OK);
  }

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with a blank document

    // Filter for the owner parameter
    if (ctx.queryParamMap().containsKey(OWNER_KEY)) {
      String targetOwner = ctx.queryParamAsClass(OWNER_KEY, String.class)
        .check(it -> it.length() > 0, "Todo's owner must have some value")
        .get();
      filters.add(regex(OWNER_KEY, targetOwner, "i"));
    }
    // Filter for the status parameter
    if (ctx.queryParamMap().containsKey(STATUS_KEY)) {
      String targetStatus = ctx.queryParamAsClass(STATUS_KEY, String.class)
        .check(it -> it.equalsIgnoreCase("complete")
        || it.equalsIgnoreCase("incomplete"), "Status parameter must be either complete or incomplete")
        .get();
      if (targetStatus.equalsIgnoreCase("complete")) {
        filters.add(eq(STATUS_KEY, true));
      } else {
        filters.add(eq(STATUS_KEY, false));
      }
    }
    // Combine the list of filters into a single filtering document.
    // if filters.isEmpty(), combinedFilter = new Document()
    // else, combinedFilter = and(filters);
    // ^ this doesn't need to be commented out, just showing what the ?: syntax does
    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  private Bson constructSortingOrder(Context ctx) {
    // Sort the results. Use the `sortby` query param (default "name")
    // as the field to sort by, and the query param `sortOrder` (default
    // "asc") to specify the sort order.
    /*String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortby"), "name");
    String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortOrder"), "asc");
    // if the sortOrder is "desc" then sortingOrder (Bson) = Sorts.descending() and whatever
    // we told Mongo to sort by (it'll be one of our HTTP parameters).
    Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);*/
    return new Document();
  }

  /**
   * Add a new todo using information from the context
   * (as long as the information gives "legal" values to Todo fields)
   *
   * @param ctx a Javalin HTTP context
   */
  /*public void addNewTodo(Context ctx) {
    /*
     * The follow chain of statements uses the Javalin validator system
     * to verify that instance of `Todo` provided in this context is
     * a "legal" todo. It checks the following things (in order):
     *    - The todo has a value for the name (`usr.name != null`)
     *    - The todo name is not blank (`usr.name.length > 0`)
     *    - The provided email is valid (matches EMAIL_REGEX)
     *    - The provided age is > 0
     *    - The provided role is valid (one of "admin", "editor", or "viewer")
     *    - A non-blank company is provided
     */
    /*
    Todo newTodo = ctx.bodyValidator(Todo.class)
      .check(usr -> usr.name != null && usr.name.length() > 0, "Todo must have a non-empty todo name")
      .check(usr -> usr.email.matches(EMAIL_REGEX), "Todo must have a legal email")
      .check(usr -> usr.age > 0, "Todo's age must be greater than zero")
      .check(usr -> usr.age < REASONABLE_AGE_LIMIT, "Todo's age must be less than " + REASONABLE_AGE_LIMIT)
      .check(usr -> usr.role.matches(ROLE_REGEX), "Todo must have a legal todo role")
      .check(usr -> usr.company != null && usr.company.length() > 0, "Todo must have a non-empty company name")
      .get();

    // Generate a todo avatar (you won't need this part for todos)
    newTodo.avatar = generateAvatar(newTodo.email);

    todoCollection.insertOne(newTodo);

    ctx.json(Map.of("id", newTodo._id));
    // 201 is the HTTP code for when we successfully
    // create a new resource (a todo in this case).
    // See, e.g., https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
    // for a description of the various response codes.
    ctx.status(HttpStatus.CREATED);
  }*/

  /**
   * Delete the todo specified by the `id` parameter in the request.
   *
   * @param ctx a Javalin HTTP context
   */
  /*public void deleteTodo(Context ctx) {
    String id = ctx.pathParam("id");
    DeleteResult deleteResult = todoCollection.deleteOne(eq("_id", new ObjectId(id)));
    if (deleteResult.getDeletedCount() != 1) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw new NotFoundResponse(
        "Was unable to delete ID "
          + id
          + "; perhaps illegal ID or an ID for an item not in the system?");
    }
    ctx.status(HttpStatus.OK);
  }*/

  /**
   * Utility function to generate the md5 hash for a given string
   *
   * @param str the string to generate a md5 for
   */
  @SuppressWarnings("lgtm[java/weak-cryptographic-algorithm]")
  public String md5(String str) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] hashInBytes = md.digest(str.toLowerCase().getBytes(StandardCharsets.UTF_8));

    StringBuilder result = new StringBuilder();
    for (byte b : hashInBytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }
}
