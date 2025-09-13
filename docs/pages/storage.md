# Clorabase storage
Storage for Clorabase is built for app developers who need to store and serve user-generated content, such as photos or videos, or any other files. It is like a cloud bucket where you can upload files that are required by the app. You can use SDK to upload, download and delete the file.

## Key features
- Max file size is 2GB
- Simple & easy SDK
- Local Filesystem type framword
- Unlimited storage & uploads
- Stors files in hierarchy

### Get the root node
```java
Clorabase clorabase = Clorabase.getInstance("token", "project");
Node root = clorabase.getClorabaseStorage();
```
then with this object, we can:

### Upload file to storage
To upload a file to a root node, use the `upload(String,InputStream,Callback)` method.

?> Tip: Take care of the read/write permission, specually if you are targeting higher android versions

```java
root.node("photos").upload("pfp.png", in, new ClorabaseStorageCallback() {
    @Override
    public void onFailed(@NonNull Exception e) {
        e.printStackTrace();
    }
    @Override
    public void onProgress(int percent) {
        System.out.println(percent);
    }
    @Override
    public void onComplete() {
        System.out.println("Comepleted");
    }
});
```
?> **Pro Tip**: To organize your files like database collections, reffered as node. A node may contains files and other nodes too.

For example, If I wanna save all images from `user1`. I will create a node for "user1" in storage, that will have the images uploaded by this user.


### Downloading files from storage
To download a file from clorabase storage, use the `download(String,File,Callback)` method.
```java
root.node("basics").download("pfp.png", out, new ClorabaseStorageCallback() {
    @Override
    public void onFailed(@NonNull Exception e) {
        e.printStackTrace();
    }
    @Override
    public void onProgress(int percent) {
        System.out.println(percent);
    }
    @Override
    public void onComplete() {
        System.out.println("Comepleted");
    }
});
```


### Delete file
Deleting file is also as easy as pie.
```java
root.node("user1").node("photos").delete("googleSearch.png", new ClorabaseStorageCallback() {
    @Override
    public void onFailed(@NonNull Exception e) {
        e.printStackTrace();
    }
    @Override
    public void onProgress(int percent) {
        System.out.println(percent);
    }
    @Override
    public void onComplete() {
        System.out.println("Comepleted");
    }
});
```

## List all the files.
```java
root.node("photos").list().forEach(System.out::println);
```


#### That's all what you need to know about the storage :)

