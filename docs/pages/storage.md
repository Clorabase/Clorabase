# Clorabase storage

Storage for Clorabase is built for app developers who need to store and serve user-generated content, such as photos or videos, or any other files. It is like a cloud bucket where you can upload files that are required by the app. You can use the SDK to upload, download and delete the file.  

## Key features

*   Max file size is 2GB
*   Simple & easy SDK
*   Local Filesystem type framework
*   Unlimited storage & uploads
*   Stors files in hierarchy

  

### Get the root node

```java
Clorabase clorabase = Clorabase.getInstance("username", "token", "project");
ClorabaseStorage root = clorabase.getStorage();
```

  
Then with this object, we can:  

### Uploading a small file to storage

To upload a small file to a directory, use the `addFileAsync(byte[],String)` method.  
  
?> Tip: Take care of the read/write permission, especially if you are targeting higher Android versions  

```java
root.addFileAsync(contents, "test.txt")
        .addOnSuccessListener(unused -> {
            // File uploaded
        })
        .addOnFailureListener(e -> {
            // Error occurred
        });
```

  
?> You can organise your files like database collections & documents, you can upload files to any sub-directory.  
  
For example, if you wanna save all images from `user1` :  

```java
root.directory("user1").addFileAsync(contents, "image1.png")
        .addOnSuccessListener(unused -> {
            // File uploaded
        })
        .addOnFailureListener(e -> {
            // Error occurred
        });
```

  

### Uploading a large file to storage (BLOB)

All the large files are stored as release assets. A pointer file is created in the respective directory pointing to the asset. The **storage feature** **must** be enabled before using this; otherwise, it will throw an exception.  
  
To upload a small file to a directory, use the uploadBlob`(InputStream,String)` method.  

```java
var in = new ByteArrayInputStream(contents);   // can be any stream
root.directory("reels").uploadBlob(in, "video1.mp4", new ProgressListener() {

    @Override
    public void onProgress(long bytesRead, long totalBytes) {
        int percent = (int) (bytesRead * 100 / totalBytes);
        // show to progress
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onError(Exception e) {

    }
});
```

  
!> Note: The progress is not accurate because the GitHub upload file endpoint does not support streaming by default.  
  

### Downloading files from storage

  
To download a small file that we uploaded earlier:  

```java
Task imageTask = root.directory("user1").getFileAsync("image1.png");
```

  
To download a BLOB, you can get an input stream of the blob using `getBlobStream()` and can use this stream to write it somewhere you want:  

```java
root.directory("reels").getBlobStreamAsync("video1.mp4")
        .addOnSuccessListener(stream -> {
            // read the stream or write it on the disk
        })
        .addOnFailureListener(e -> {
            // Check the error
        });
```

  
  

### Delete file

Deleting a file is so easy; you just need to call `deleteAsync("filename")` on the directory

```java
Task task1 = root.directory("reels").deleteBlobAsync("video1.mp4");
Task task2 = root.directory("user1").deleteAsync("image1.png");
```

  

## List all the files.

```java
root.directory("reels").listFilesAsync()
        .addOnSuccessListener(files -> {
            // List of files
        })
        .addOnFailureListener(e -> {
            // Error occurred
        });
```

  
  

#### That's all you need to know about the storage :)
