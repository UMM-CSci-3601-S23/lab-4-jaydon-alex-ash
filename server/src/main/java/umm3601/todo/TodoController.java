package umm3601.user;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

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
  static final Boolean STATUS_KEY = "status";
  static final String BODY_KEY = "body";
  static final String CATEGORY_STRING = "category";


  private final JacksonMongoCollection<Todo> todoCollection;

  /**
   * Construct a controller for todos.
   *
   * @param database the database containing user data
   */
  public TodoController(MongoDatabase database) {
    todoCollection = JacksonMongoCollection.builder().build(
        database,
        "todos",
        Todo.class,
        UuidRepresentation.STANDARD);
  }

  /**
   * Set the JSON body of the response to be the single user
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
   * Set the JSON body of the response to be a list of all the users returned from the database
   * that match any requested filters and ordering
   *
   * @param ctx a Javalin HTTP context
   */
  public void getTodos(Context ctx) {
    Bson combinedFilter = constructFilter(ctx);
    Bson sortingOrder = constructSortingOrder(ctx);

    // All three of the find, sort, and into steps happen "in parallel" inside the
    // database system. So MongoDB is going to find the users with the specified
    // properties, return those sorted in the specified manner, and put the
    // results into an initially empty ArrayList.
    ArrayList<Todo> matchingTodos = todoCollection
      .find(combinedFilter)
      .sort(sortingOrder)
      // .into seems to be telling Mongo how it will format the data returned (as an
      // ArrayList<>)
      .into(new ArrayList<>());

    // Set the JSON body of the response to be the list of users returned by the database.
    // According to the Javalin documentation (https://javalin.io/documentation#context),
    // this calls result(jsonString), and also sets content type to json
    ctx.json(matchingTodos);

    // Explicitly set the context status to OK
    ctx.status(HttpStatus.OK);
  }

  private Bson constructFilter(Context ctx) {
    List<Bson> filters = new ArrayList<>(); // start with a blank document

   if (ctx.queryParamMap().containsKey(OWNER_KEY)) {
     
      filters.add(regex(OWNER_KEY,  Pattern.quote(ctx.queryParam(OWNER_KEY)), "i"));
    }
    if (ctx.queryParamMap().containsKey(STATUS_KEY)) {
      
      filters.add(regex(STATUS_KEY,  Pattern.quote(ctx.queryParam(STATUS_KEY)), "i"));
    }
    if (ctx.queryParamMap().containsKey(BODY_KEY)) {
     
      filters.add(regex(BODY_KEY,  Pattern.quote(ctx.queryParam(BODY_KEY)), "i"));
    }

    // Combine the list of filters into a single filtering document.
    // if filters.isEmpty(), combinedFilter = new Document()
    // else, combinedFilter = and(filters);
    Bson combinedFilter = filters.isEmpty() ? new Document() : and(filters);

    return combinedFilter;
  }

  private Bson constructSortingOrder(Context ctx) {
    // Sort the results. Use the `sortby` query param (default "name")
    // as the field to sort by, and the query param `sortorder` (default
    // "asc") to specify the sort order.
    String sortBy = Objects.requireNonNullElse(ctx.queryParam("sortby"), "name");
    String sortOrder = Objects.requireNonNullElse(ctx.queryParam("sortorder"), "asc");
    // if the sortOrder is "desc" then sortingOrder (Bson) = Sorts.descending() and whatever
    // we told Mongo to sort by (it'll be one of our HTTP parameters).
    Bson sortingOrder = sortOrder.equals("desc") ?  Sorts.descending(sortBy) : Sorts.ascending(sortBy);
    return sortingOrder;
  }

  /**
   * Add a new user using information from the context
   * (as long as the information gives "legal" values to User fields)
   *
   * @param ctx a Javalin HTTP context
   */
  public void addNewUser(Context ctx) {
    /*
     * The follow chain of statements uses the Javalin validator system
     * to verify that instance of `User` provided in this context is
     * a "legal" user. It checks the following things (in order):
     *    - The user has a value for the name (`usr.name != null`)
     *    - The user name is not blank (`usr.name.length > 0`)
     *    - The provided email is valid (matches EMAIL_REGEX)
     *    - The provided age is > 0
     *    - The provided role is valid (one of "admin", "editor", or "viewer")
     *    - A non-blank company is provided
     */
    User newUser = ctx.bodyValidator(User.class)
      .check(usr -> usr.name != null && usr.name.length() > 0, "User must have a non-empty user name")
      .check(usr -> usr.email.matches(EMAIL_REGEX), "User must have a legal email")
      .check(usr -> usr.age > 0, "User's age must be greater than zero")
      .check(usr -> usr.age < REASONABLE_AGE_LIMIT, "User's age must be less than " + REASONABLE_AGE_LIMIT)
      .check(usr -> usr.role.matches(ROLE_REGEX), "User must have a legal user role")
      .check(usr -> usr.company != null && usr.company.length() > 0, "User must have a non-empty company name")
      .get();

    // Generate a user avatar (you won't need this part for todos)
    newUser.avatar = generateAvatar(newUser.email);

    userCollection.insertOne(newUser);

    ctx.json(Map.of("id", newUser._id));
    // 201 is the HTTP code for when we successfully
    // create a new resource (a user in this case).
    // See, e.g., https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
    // for a description of the various response codes.
    ctx.status(HttpStatus.CREATED);
  }

  /**
   * Delete the user specified by the `id` parameter in the request.
   *
   * @param ctx a Javalin HTTP context
   */
  public void deleteUser(Context ctx) {
    String id = ctx.pathParam("id");
    DeleteResult deleteResult = userCollection.deleteOne(eq("_id", new ObjectId(id)));
    if (deleteResult.getDeletedCount() != 1) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw new NotFoundResponse(
        "Was unable to delete ID "
          + id
          + "; perhaps illegal ID or an ID for an item not in the system?");
    }
    ctx.status(HttpStatus.OK);
  }

  /**
   * Utility function to generate an URI that points
   * at a unique avatar image based on a user's email.
   *
   * This uses the service provided by gravatar.com; there
   * are numerous other similar services that one could
   * use if one wished.
   *
   * @param email the email to generate an avatar for
   * @return a URI pointing to an avatar image
   */
  private String generateAvatar(String email) {
    String avatar;
    try {
      // generate unique md5 code for identicon
      avatar = "https://gravatar.com/avatar/" + md5(email) + "?d=identicon";
    } catch (NoSuchAlgorithmException ignored) {
      // set to mystery person
      avatar = "https://gravatar.com/avatar/?d=mp";
    }
    return avatar;
  }

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
