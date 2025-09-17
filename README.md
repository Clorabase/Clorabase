<p align="center"><img alt="clorabase" height="300" src="/Banner.png"></p>
<p align="center">
  <img src="https://img.shields.io/github/license/ErrorxCode/Clorabase?style=for-the-badge">
  <img src="https://img.shields.io/github/stars/ErrorxCode/Clorabase?style=for-the-badge">
  <img src="https://img.shields.io/github/issues/ErrorxCode/Clorabase?color=red&style=for-the-badge">
  <img src="https://img.shields.io/github/forks/ErrorxCode/Clorabase?color=teal&style=for-the-badge">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Author-Rahil--Khan-cyan?style=flat-square">
  <img src="https://img.shields.io/badge/Open%20Source-Yes-cyan?style=flat-square">
  <img src="https://img.shields.io/badge/Written%20In-Java-cyan?style=flat-square">
  <img src="https://jitpack.io/v/Clorabase/Clorabase.svg">
</p>


## Clorabase
> *Turn your github repo into a No-SQL database*

Clorabase is a free backend alternative (BaaS) for Android and java apps that uses GitHub API & Github repo to facilitate NoSQL database & NTFS like storage system. Clorabase is mostly made for small apps which has small backend/server/database requirements. If your app scales large or you need more database storage or bandwidth, Clorabase may not serve you. You should consider using [Firebase](https://firebase.google.com). Clorabase AIMS provides a money-free production-ready backend for building Android apps as a hobby or start-up. It is for those who don't earn money from their apps and build apps just for learning or non-profit use.

<a href="https://github.com/Clorabase/Clorabase/releases/download/0.5/Console-stable-v0.5.apk"> <img alt="Download console" height=40 src="/button.png"></a>

## Features
- No account needed
- Uses GitHub usage & quota
- Absolutely Free, No paid plans
- Serverless NO-SQL database
- In-app messaging
- In-app updates
- Cloud storage for apps


## Implementation
#### To add SDK
In your project **build.gradle**
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
In module **build.gradle** inside **dependencies** block:
- For Android SDK:
```
implementation 'com.github.Clorabase.Clorabase:SDK:0.5'
```
- For Java SDK:
```
implementation 'com.github.Clorabase.Clorabase:Clorabase4j:0.5'
```



#### To generate a GitHub OAuth token
1. Goto [Create token](https://github.com/settings/tokens/new)
2. Select the 'Classic' token type and set the expiry to 'No expiration'
3. Check the following in the scope sections:
   - delete_repo
   - repo
4. Click generate.

Now you can use this token to access the console and SDK.

⚠️**NOTE:** Never publish your code containing this token on GitHub, if you do, then the token will automatically be deleted and your code will break


### Documentation
- [User guide](https://clorabase.github.io)

## Clorabase vs Firebase
The rule of thumb is, if you’re building a small project or dedicated mobile application, and you don’t mind the high bandwidth or database storage, Clorabase is a great place to start. If you’re looking for a more general-purpose data solution, value performance at scale and advanced querying, Firebase is going to serve your needs best.

See the table below to compare Clorabase and Firebase with their features.

| Feature | Clorabase | Firebase |
| :--- | :--- | :--- |
| **Primary Use Case** | Small, non-commercial Android apps, hobby projects, proof-of-concepts, learning. | Professional, scalable, and cross-platform applications (mobile & web). |
| **Cost** | **Absolutely free, no paid plans or usage barriers.** | **Freemium model:** Generous free tier, but a "pay-as-you-go" plan for high usage. |
| **Backend Storage** | Your data is stored in your own **GitHub repository** | Data is stored on **Google's cloud infrastructure** (Firestore, Realtime Database, Cloud Storage). |
| **Scalability** | **Limited.** Best for small-scale applications. It may not perform well with large amounts of traffic or data. | **Highly Scalable.** Built to handle millions of concurrent connections and terabytes of data. |
| **Core Services** | Databases (NoSQL), Cloud Storage, Push Messaging, In-App Messaging, In-App Updates. | Databases (Firestore, Realtime DB), Authentication, Cloud Functions, Hosting, Machine Learning, Analytics, Push Notifications, and more. |


#### Choose Clorabase when...
   -   Your primary concern is **cost**, and you need a completely free solution.
        
   -   You are building a personal or hobby project that won't have a large user base.
        
-   The app is an Android or Java only project.
        
  -   You have a simple data model and don't require advanced queries, authentication, or other integrated services.
        
-    You are comfortable with a more hands-on, DIY approach and are willing to handle things like user authentication yourself.



## Contributing to Clorabase
First off, thank you for considering contributing to Clorabase! It's because of people like you that this project can be what it is today.

We welcome all types of contributions, from reporting bugs and suggesting new features to writing code and improving documentation.

 Refer to [contribution.md](/contribution.md) to start contributing

*-> For personal assitance, you can contact the repo owner at x0.khanrahil@gmail.com or [x1.rahil](https://instagram.com/x1.rahil) on instagram*



## Support
This project comprises of lot's of energy, brainstorming, coding, testing and time. If this project has helped you in any way, please consider giving it a ⭐ to show your support and help it grow! Also, *watch* the repo to notify about updates.

[![Readme Quotes](https://quotes-github-readme.vercel.app/api?type=horizontal&theme=dracula)](https://github.com/piyushsuthar/github-readme-quotes)


![water](https://raw.githubusercontent.com/mayhemantt/mayhemantt/Update/svg/Bottom.svg)
