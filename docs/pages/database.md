# Clorabase Database

Clorabase Database is an open-source, flexible, serverless database for Android & Java apps built on top of [ClorastoreDB](https://github.com/Clorabase/ClorastoreDB) that uses GitHub API to facilitate your online database. It has a dedicated SDK for Android apps written on top of a Java library. The SDK uses Google's Tasks API as a wrapper of a Java library, which helps you to write async code.  

## Key features

*   No extra account is needed
*   Simple & easy SDK
*   NO-SQL databases
*   Can be managed using the console and Github web UI

  

## Usage / Examples

This database is implemented from the [ClorastoreDB](https://github.com/Clorabase/ClorastoreDB) offline version. See its docs to understand the pattern of the database.  
Refer to the [Wikipedia article](https://en.wikipedia.org/wiki/Document-oriented_database) to learn more about this.  
  

### Get the root collection of the database

```java
Clorabase clorabase = Clorabase.getInstance(GITHUB_USERNAME,GITHUB_TOKEN, PROJECT_NAME);
Collection root = clorabase.getDatabase();
```

  
To get your Oauth token, check the [Implementation](https://github.com/ErrorxCode/docs/edit/main/clorabase/README.md#implementation) part of the README  

[!NOTE] 
> **In Android SDK:** Every method has its `async` version and `non-async` version. All the async methods end with `async` and return `async`. Implementation of the class.  
> **In the Java library:** There are only sync functions with the same name  

### Writing/Updating data

Let's insert a new user into `users` collection :

```java
Map data = new HashMap<>();
data.put("name","John");
data.put("age",25);
data.put("is_married",false);

root.collection("users").documentAsync("user1").setDataAsync(data)
        .addOnSuccessListener(unused -> System.out.println("Success"))
        .addOnFailureListener(e -> System.out.println("Failed"));          
```

  
If the `user1` document already exists in that collection, **then it will update its fields.**  
  
?> **Tip**  
\>Every database operation method returns a `Task` . See tasks [documentation](https://developers.google.com/android/guides/tasks) for more info.  
  
The structure created will be like this,  
  
![image](https://user-images.githubusercontent.com/65817230/230773260-1a207a69-03e6-4c3a-9fca-d4f0bba305c3.png)  
  
  
You can also have a collection inside a collection, but not a collection or document inside a document.  
  

### Reading data

To read data from the database, use `getDataAsync()` method on the **document** where it was inserted.

```java
root.collection("users").documentAsync("user1").getDataAsync()
        .addOnSuccessListener(System.out::println)
        .addOnFailureListener(Throwable::printStackTrace);
```

  

### Deleting data

To delete a document or collection, go to its parent collection and call `delete()method. To delete a field in a node, just put its value to null`

```java
root.collection("users").deleteAsync("user1")
        .addOnSuccessListener(System.out::println)
        .addOnFailureListener(Throwable::printStackTrace);
```

  
  

### Querying data

Querying data in **ClorastoreDB** is as easy as pie. You just have to pass a `Predicate as a condition for querying data. The database will include every document for which the Predicate` return true.  
  
Suppose if your structure is like this (\*JSON representation of collection & it's document, where `users is the collection and each user` object is it's document\*):

```plain
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

  

*   To get all the users whose age is greater than 18,

```plain
root.collection("users").query().whereGreater("age",18)
```

  

*   To get users whose name start's with 'a'

```plain
db.collection("users").query().where(data -> data.get("name").toString().startsWith("a"));
```

  
**Note**: You need to manually check for the `return value of data.get("age")` , as it could be null if some document does not contain that value.  

*   To order data by a particular field (Ascending order)

```plain
root.collection("users").query().orderBy("age",20);
```

  
  
  
_That's all that you need to know about the database._






