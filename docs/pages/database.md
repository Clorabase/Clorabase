
# Clorabase Database
Clorabase Database is an open-source, flexible, serverless database for Android apps built on top of GitHub, which turns it into a backend. It offers offline support for Android apps so you can build responsive apps that work regardless of network latency or Internet connectivity. Clorabase offers 2 kinds of databases, one is [ClorastoreDB](https://github.com/Clorabase/ClorastoreDB) which is a NO-SQL document-oriented database and another one is [ClorographDB](https://github.com/Clorabase/ClorographDB) that is a graph database.

## Key features
- No account is needed
- Simple & easy SDK
- NO-SQL databases

## ClorastoreDB
This database is implemented from [ClorastoreDB](https://github.com/Clorabase/ClorastoreDB) offline version. See its docs to understand the pattern of the database.
Refer to [Wikipedia article](https://en.wikipedia.org/wiki/Document-oriented_database) to learn more about this.


### Get the root collection of the database

```java
Clorabase clorabase = Clorabase.getInstance(GITHUB_TOKEN, PROJECT_NAME);
Collection root = clorabase.getClorastoreDatabase();
```
To get your Oauth token, check the [Implementation](https://github.com/ErrorxCode/docs/edit/main/clorabase/README.md#implementation) part of the README


### Writing/Updating data
Let's insert a new user into `users` collection :
```java
Map<String,Object> data = new HashMap<>();
data.put("name","John");
data.put("age",25);
data.put("is_married",false);

root.collection("users").document("user1").setData(data)
        .addOnSuccessListener(unused -> System.out.println("Success"))
        .addOnFailureListener(e -> System.out.println("Failed"));          
```
If the `user1` document already exists in that collection, **then it will update its fields.**

?> **Tip** 
>Every database operation method returns a `Task`. See tasks [documentation](https://developers.google.com/android/guides/tasks) for more info.

The structure created will be like this,

![image](https://user-images.githubusercontent.com/65817230/230773260-1a207a69-03e6-4c3a-9fca-d4f0bba305c3.png)


You can also have a collection inside a collection, but not a collection or document inside a document.


### Reading data
To read data from database, use `getData()` method on the **document** where it was inserted.
```java
root.collection("users").document("user1").getData()
        .addOnSuccessListener(System.out::println)
        .addOnFailureListener(Throwable::printStackTrace);
```

### Deleting data
To delete a document or collection, go to its parent collection and call `delete()`method. To delete a field in a node, just put its value to `null`
```java
root.collection("users").delete("user1")
        .addOnSuccessListener(System.out::println)
        .addOnFailureListener(Throwable::printStackTrace);
```


### Querying data
Querying data in **ClorastoreDB** is as easy as pie. You just have to pass a `Predicate` as a condition for querying data. The database will include every document for which the `Predicate` returns true.

Suppose if your structure is like this (*JSON representation of collection & it's document, where `users` is the collection and each `user` object is it's document*):
```JSON
{
  "users": [
    "user1": {
      "name": "John",
      "age": 30,
      "is_married": false
    },
    "user2": {
      "name": "Mary",
      "age": 25,
      "is_married": true
    },
    "user3": {
      "name": "Mike",
      "age": 27,
      "is_married": false
    }
  ]
}
```
- To get all the users whose age is greater then 18,
```java
root.collection("users").query().whereGreater("age",18)
```
- To get users whose name start's with 'a'
```java
db.collection("users").query().where(data -> data.get("name").toString().startsWith("a"));
```
**Note** : You need to manually check for the `return` value of `data.get("age")`, as it could be null if some document does not contain that value.

- To order data by a perticular field (Accending order)
```java
root.collection("users").query().orderBy("age",20);
```


_That's all that you need to know about the database._
