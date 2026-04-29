<h1 align="center">
  <img src="/clorabase-banner.png" alt="Clorabase" width="800">
</h1>

<h4 align="center">Turn your GitHub repo into a No-SQL database.</h4>

<p align="center">
  <img src="https://img.shields.io/badge/Version-0.6-green?style=for-the-badge">
  <img src="https://img.shields.io/github/stars/Clorabase/Clorabase?style=for-the-badge">
  <img src="https://img.shields.io/github/issues/Clorabase/Clorabase?color=red&style=for-the-badge">
  <img src="https://img.shields.io/github/forks/Clorabase/Clorabase?color=teal&style=for-the-badge">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Author-ErrorxCode-cyan?style=flat-square">
  <img src="https://img.shields.io/badge/Open%20Source-Yes-green?style=flat-square">
  <img src="https://img.shields.io/badge/Made%20In-Java-orange?style=flat-square">
</p>


<p align="center">
  <img src="https://jitpack.io/v/Clorabase/Clorabase/month.svg">
</p>

---

**Clorabase** is a lightweight, serverless **Backend-as-a-Service (BaaS)** designed for Android and Java applications. It leverages **GitHub** as both a storage and database provider, enabling developers to host application data, manage files, and handle in-app communications without traditional cloud hosting or complex server setups.

It is ideal for **small apps, hobby projects, and non-commercial use cases** where cost efficiency and simplicity matter most. For larger-scale apps requiring advanced querying, authentication, or analytics, consider [Firebase](https://firebase.google.com).


## 🚀 Features

- **Clorastore Database**  
  A NoSQL-like database storing data as **encrypted JSON files** in a GitHub repository.  
  - Supports nested collections  
  - Document-based CRUD operations  
  - Basic querying

- **Integrated Storage**  
  Two modes of file management:  
  - Standard uploads (< 50 MB) directly to the repo  
  - Large file uploads via **GitHub Release assets**

- **In-App Messaging (Android)**  
  Remote messaging service for showing custom dialogs (Coupon, Promo, Simple) by updating a JSON file in the repo.

- **In-App Updates (Android)**  
  Version management utility for apps outside the Play Store.  
  - Flexible or immediate update prompts  
  - Controlled via `version.json`

- **Security**  
  All records are encrypted with **AES** before being pushed to GitHub.

- **Zero Cost**  
  Fully free solution powered by GitHub’s infrastructure — ideal for small to medium projects.

- **Other Highlights**  
  - No account needed  
  - Uses GitHub usage & quota  
  - Absolutely free, no paid plans  
  - Cloud storage for apps  

## ⚙️ Implementation

In your project **build.gradle**:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
In module **build.gradle** inside **dependencies** block:

-   For Android SDK:

```gradle
implementation 'com.github.Clorabase.Clorabase:Clorabase4A:0.6'
```

-   For Java SDK:

```gradle
implementation 'com.github.Clorabase.Clorabase:Clorabase4j:0.6'
```


## 🚀 Quick Start / Usage

### 1️⃣ Initialization
Initialize the `Clorabase` singleton instance as early as possible in your app (e.g., in your `Application` class or the `onCreate` of your main Activity).

```java
try {
    // Replace with your GitHub credentials and project name
    Clorabase clorabase = Clorabase.getInstance("username", "token", "project");
} catch (Exception e) {
    e.printStackTrace();
}
```

### 2️⃣ Basic Operations

**📂 Database (Clorastore)**

```java
// 1. Get the root database reference
ClorastoreCollection db = clorabase.getDatabase();

// 2. Prepare your data
Map<String, Object> data = new HashMap<>();
data.put("name", "Rahil");
data.put("role", "Admin");

// 3. Write data to a specific document asynchronously
db.collection("users").document("user1")
  .setData(data)
  .addOnSuccessListener(v -> Log.d("DB", "Data saved successfully!"));
```

**📦 Storage**

```java
// 1. Get the storage reference
ClorabaseStorage storage = clorabase.getStorage();

// 2. Create or navigate to a specific directory
ClorabaseStorage imagesDir = storage.directory("images");

// 3. Upload a file stream
imagesDir.uploadFile(inputStream, "avatar.png", new ProgressListener() {
    @Override
    public void onProgress(long bytesRead, long totalBytes) {
        // Track progress here
    }
    @Override
    public void onComplete() {
        Log.d("Storage", "Upload complete!");
    }
    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
});
```

#### For detailed user guide, visit our [documentation]()


## 🔑 Generating GitHub Token

Clorabase requires a **GitHub Personal Access Token (PAT)**:

1.  Navigate to **Settings** > **Developer settings** > **Personal access tokens** > **Tokens (classic)**.
2.  Click **Generate new token**.
3.  Select scopes:
    -   `repo` (full control of private repositories)
4.  Copy and store the token securely — it’s required for SDK initialization.

⚠️ **Note:** Never publish your token in public repos. If exposed, GitHub will revoke it automatically.


----

## 📊 Clorabase vs Firebase


| Feature | Clorabase | Firebase |
| --- | --- | --- |
| **Use Case** | Small, non-commercial Android apps, hobby projects, proof-of-concepts | Professional, scalable, cross-platform apps |
| **Cost** | 100% Free | Tiered / Pay-as-you-go |
| **Data Ownership** | Stored on your GitHub repo | Stored on Google Servers |
| **Setup Complexity** | Very Low | Moderate to High |
| **Database Type** | NoSQL | NoSQL |
| **Storage support** | Samll file and Large file | Unified single storage |
| **In-App Messages** | Supported (Simple) | Supported (Customizable) |
| **In-app update**   | Supported | Absent   |
| **Authentication**  | Absent | Supported |
| **Core Services** | Database, Storage, Messaging, Updates | Database, Auth, Hosting, ML, Analytics, Functions |
| **Scalability** | Limited, best for small-scale apps | Highly scalable, millions of users |



### ✅ Choose Clorabase when...

-   Your primary concern is **cost** (completely free).
-   You’re building a **personal or hobby project** with a small user base.
-   The app is **Android/Java only**.
-   You have a **simple data model** without advanced queries or authentication needs.
-   You prefer a **DIY approach** and don’t mind handling authentication yourself.




## 🤝 Contribution & Support

Clorabase is an open-source project by **Rahil Khan**. Contributions are welcome!

-   **Report Bugs** → Use the GitHub Issues tab
-   **Support** → Refer to upcoming Wiki pages or community discussions
-   **Contribution Guide** → See [contribution.md](https://copilot.microsoft.com/contribution.md)

For personal assistance, contact:  
📧 x0.khanrahil@gmail.com  
📱 [Instagram: @x1.rahil](https://instagram.com/x1.rahil)


## ⭐ Support the Project

This project took a lot of energy, brainstorming, coding, and testing.  
If Clorabase helped you, please **give it a star ⭐** and **watch** the repo to stay updated!


[![Readme Quotes](https://quotes-github-readme.vercel.app/api?type=horizontal&theme=dracula)](https://github.com/piyushsuthar/github-readme-quotes)
