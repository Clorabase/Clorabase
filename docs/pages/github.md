# Managing project/app

  
The main feature of clorabase is that it turns your GitHub repo into an application database. Since all the data is stored within your repo contents, you can also manage it using the GitHub Web UI, just like you do for your other projects on GitHub. However, due to a custom protocol & specification that clorabase uses inside, you cannot do everything from the GitHub UI that you can do from the console app.  
  

## What things can you do using GitHub UI

*   Upload a small file to storage
*   Insert/Send an in-app message to the queue
*   Create a versioned app update
*   Delete database documents/collection
*   Delete storage files
*   Delete project
*   Edit in-app messages/updates
*   Edit or change project configurations

  

### Uploading a file into storage

To upload a file to your project storage, you can directly go to the project storage directory and upload a file there.  
The project storage directory can be found inside your project folder from the root of the "Clorabase-database" repo.  
Eg. `foobar/storage/user1/photos` --> Upload a file here  
  

### Send in-app message

To send an in-app message, just create/upload a JSON file that contains the message spec, with a random name, into the project's messaging directory, i.e `project/messages`  
  
**Example message.json:**  

```json
{
  "title": "Demo title",
  "message": "Demo message...",
  "link": "https://errorxcode.github.io/clorabase",
  "type": "simple"
}
```

  
**Note: All the fields are mandatory**  
  

### Create a versioned app

All the updates related information is created and managed in the update directory of your project. In this directory, there are folders with the package name of your Android application, which then contain the information about the current version of the app.  
  
To add a versioned application to your project, just create a `versions.json` inside your app package folder, in the project's updates directory, i.e `project/updates/com.foo.bar`  
  
**Example versions.json:**  

```json
{
  "code": 5,
  "name": "4dgg",
  "package": "clorabase.sample.app",
  "downloadUrl": "https://google.com",
  "downloadCount": 0,
  "mode": "flexible",
  "date": "12-09-2025"
}
```

  
  
  

### Deleting document/file/message or anything:

Deleting is very easy; you just need to delete the respective files from the respective directory. For example, you can delete any document from the database, any message from the message queue or any files from the storage. _There's nothing much you have to take care of while deleting_  
  
  

> _That's all, Thank you for paying attention to the docs :)_
