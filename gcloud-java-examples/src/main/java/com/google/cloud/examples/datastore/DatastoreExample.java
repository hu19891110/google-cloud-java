/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.datastore;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DateTime;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An example of using Google Cloud Datastore.
 *
 * <p>This example adds, displays or clears comments for a given user. This example also sets
 * contact information for a user.
 *
 * <p>Steps needed for running the example:<ol>
 * <li>login using gcloud SDK - {@code gcloud auth login}.</li>
 * <li>compile using maven - {@code mvn compile}</li>
 * <li>run using maven -
 * <pre>{@code mvn exec:java -Dexec.mainClass="com.google.cloud.examples.datastore.DatastoreExample"
 *  -Dexec.args="<project_id> <user>
 *  delete |
 *  display |
 *  add <comment> |
 *  set <email> <phone>}</pre>
 * </li>
 * </ol>
 *
 * <p>If no action is provided {@code display} is executed.
 */
public class DatastoreExample {

  private static final String USER_KIND = "_DS_EXAMPLE_USER";
  private static final String COMMENT_KIND = "_DS_EXAMPLE_COMMENT";
  private static final String NAMESPACE = "gcloud_java_example";
  private static final String DEFAULT_ACTION = "display";
  private static final Map<String, DatastoreAction> ACTIONS = new HashMap<>();

  private abstract static class DatastoreAction<T> {

    abstract void run(Transaction tx, Key userKey, T arg) throws Exception;

    abstract T parse(String... args) throws Exception;

    protected String params() {
      return "";
    }
  }

  /**
   * This class demonstrates how to delete a user. This action also queries the keys of all comments
   * associated with the user and uses them to delete comments.
   */
  private static class DeleteAction extends DatastoreAction<Void> {
    @Override
    public void run(Transaction tx, Key userKey, Void arg) {
      Entity user = tx.get(userKey);
      if (user == null) {
        System.out.println("Nothing to delete, user does not exist.");
        return;
      }
      Query<Key> query = Query.keyQueryBuilder()
          .namespace(NAMESPACE)
          .kind(COMMENT_KIND)
          .filter(PropertyFilter.hasAncestor(userKey))
          .build();
      QueryResults<Key> comments = tx.run(query);
      int count = 0;
      while (comments.hasNext()) {
        tx.delete(comments.next());
        count++;
      }
      tx.delete(userKey);
      System.out.printf("Deleting user '%s' and %d comment[s].%n", userKey.name(), count);
    }

    @Override
    Void parse(String... args) throws Exception {
      return null;
    }
  }

  /**
   * This class demonstrates how to get a user. The action also queries all comments associated
   * with the user.
   */
  private static class DisplayAction extends DatastoreAction<Void> {
    @Override
    public void run(Transaction tx, Key userKey, Void arg) {
      Entity user = tx.get(userKey);
      if (user == null) {
        System.out.printf("User '%s' does not exist.%n", userKey.name());
        return;
      }
      if (user.contains("contact")) {
        FullEntity<IncompleteKey> contact = user.getEntity("contact");
        String email = contact.getString("email");
        String phone = contact.getString("phone");
        System.out.printf("User '%s' email is '%s', phone is '%s'.%n",
            userKey.name(), email, phone);
      }
      System.out.printf("User '%s' has %d comment[s].%n", userKey.name(), user.getLong("count"));
      int limit = 200;
      Map<DateTime, String> sortedComments = new TreeMap<>();
      StructuredQuery<Entity> query =
          Query.entityQueryBuilder()
              .namespace(NAMESPACE)
              .kind(COMMENT_KIND)
              .filter(PropertyFilter.hasAncestor(userKey))
              .limit(limit)
              .build();
      while (true) {
        QueryResults<Entity> results = tx.run(query);
        int resultCount = 0;
        while (results.hasNext()) {
          Entity result = results.next();
          sortedComments.put(result.getDateTime("timestamp"), result.getString("content"));
          resultCount++;
        }
        if (resultCount < limit) {
          break;
        }
        query = query.toBuilder().startCursor(results.cursorAfter()).build();
      }
      // We could have added "ORDER BY timestamp" to the query to avoid sorting, but that would
      // require adding an ancestor index for timestamp.
      // See: https://cloud.google.com/datastore/docs/tools/indexconfig
      for (Map.Entry<DateTime, String> entry : sortedComments.entrySet()) {
        System.out.printf("\t%s: %s%n", entry.getKey(), entry.getValue());
      }
    }

    @Override
    Void parse(String... args) throws Exception {
      return null;
    }
  }

  /**
   * This class adds a comment for a user. If the user does not exist its entity is created.
   */
  private static class AddCommentAction extends DatastoreAction<String> {
    @Override
    public void run(Transaction tx, Key userKey, String content) {
      Entity user = tx.get(userKey);
      if (user == null) {
        System.out.println("Adding a new user.");
        user = Entity.builder(userKey).set("count", 1).build();
        tx.add(user);
      } else {
        user = Entity.builder(user).set("count", user.getLong("count") + 1L).build();
        tx.update(user);
      }
      IncompleteKey commentKey = IncompleteKey.builder(userKey, COMMENT_KIND).build();
      FullEntity<IncompleteKey> comment = FullEntity.builder(commentKey)
          .set("content", content)
          .set("timestamp", DateTime.now())
          .build();
      tx.addWithDeferredIdAllocation(comment);
      System.out.printf("Adding a comment to user '%s'.%n", userKey.name());
    }

    @Override
    String parse(String... args) throws Exception {
      String content = "No comment.";
      if (args.length > 0) {
        StringBuilder stBuilder = new StringBuilder();
        for (String arg : args) {
          stBuilder.append(arg).append(' ');
        }
        stBuilder.setLength(stBuilder.length() - 1);
        content = stBuilder.toString();
      }
      return content;
    }

    @Override
    protected String params() {
      return "<comment>";
    }
  }

  /**
   * This class sets contact information (email and phone) for a user. If the user does not exist
   * its entity is created. Contact information is saved as an entity embedded in the user entity.
   */
  private static class SetContactAction extends DatastoreAction<SetContactAction.Contact> {

    static final class Contact {

      private final String email;
      private final String phone;

      Contact(String email, String phone) {
        this.email = email;
        this.phone = phone;
      }

      String email() {
        return email;
      }

      String phone() {
        return phone;
      }
    }

    @Override
    public void run(Transaction tx, Key userKey, Contact contact) {
      Entity user = tx.get(userKey);
      if (user == null) {
        System.out.println("Adding a new user.");
        user = Entity.builder(userKey).set("count", 0L).build();
        tx.add(user);
      }
      FullEntity<IncompleteKey> contactEntity = FullEntity.builder()
          .set("email", contact.email())
          .set("phone", contact.phone())
          .build();
      tx.update(Entity.builder(user).set("contact", contactEntity).build());
      System.out.printf("Setting contact for user '%s'.%n", userKey.name());
    }

    @Override
    Contact parse(String... args) throws Exception {
      String message;
      if (args.length == 2) {
        return new Contact(args[0], args[1]);
      } else if (args.length > 2) {
        message = "Too many arguments.";
      } else {
        message = "Missing required email and phone.";
      }
      throw new IllegalArgumentException(message);
    }

    @Override
    protected String params() {
      return "<email> <phone>";
    }
  }

  static {
    ACTIONS.put("delete", new DeleteAction());
    ACTIONS.put("add", new AddCommentAction());
    ACTIONS.put("set", new SetContactAction());
    ACTIONS.put("display", new DisplayAction());
  }

  private static void printUsage() {
    StringBuilder actionAndParams = new StringBuilder();
    for (Map.Entry<String, DatastoreAction> entry : ACTIONS.entrySet()) {
      actionAndParams.append("\n\t").append(entry.getKey());

      String param = entry.getValue().params();
      if (param != null && !param.isEmpty()) {
        actionAndParams.append(' ').append(param);
      }
    }
    System.out.printf("Usage: %s <project_id> <user> operation <args>*%s%n",
        DatastoreExample.class.getSimpleName(), actionAndParams);
  }

  @SuppressWarnings("unchecked")
  public static void main(String... args) throws Exception {
    String projectId = args.length > 0 ? args[0] : null;
    // If you want to access a local Datastore running via the Google Cloud SDK, do
    //   DatastoreOptions options = DatastoreOptions.builder()
    //       .projectId(projectId)
    //       .namespace(NAMESPACE)
    //       .host("http://localhost:8080") // change 8080 to the port that the emulator listens to
    //       .build();
    DatastoreOptions options =
        DatastoreOptions.builder().projectId(projectId).namespace(NAMESPACE).build();
    String name = args.length > 1 ? args[1] : System.getProperty("user.name");
    Datastore datastore = options.service();
    KeyFactory keyFactory = datastore.newKeyFactory().kind(USER_KIND);
    Key key = keyFactory.newKey(name);
    String actionName = args.length > 2 ? args[2].toLowerCase() : DEFAULT_ACTION;
    DatastoreAction action = ACTIONS.get(actionName);
    if (action == null) {
      System.out.println("Unrecognized action.");
      printUsage();
      return;
    }
    args = args.length > 3 ? Arrays.copyOfRange(args, 3, args.length) : new String []{};
    Transaction tx = datastore.newTransaction();
    Object request;
    try {
      request = action.parse(args);
    } catch (IllegalArgumentException ex) {
      System.out.printf("Invalid input for action '%s'. %s%n", actionName, ex.getMessage());
      System.out.printf("Expected: %s%n", action.params());
      return;
    } catch (Exception ex) {
      System.out.println("Failed to parse request.");
      ex.printStackTrace();
      return;
    }
    try {
      action.run(tx, key, request);
      tx.commit();
    } finally {
      if (tx.active()) {
        tx.rollback();
      }
    }
  }
}