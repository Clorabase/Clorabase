<p align="center"><img alt="clorographDB" height="350" width="350" src="/clorabase.png"></p>
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
</p>


### Clorabase
Clorabase is a free backend alternative (BaaS) for Android apps that uses GitHub API to facilitate backend features. Clorabase is mostly made for small apps which has small backend/server/database requirements. If your app scales large or you need more database storage or bandwidth, Clorabase may not serve you. You should consider using [Firebase](https://firebase.google.com). Clorabase AIMS provides a money-free production-ready backend for building Android apps as a hobby or start-up. It is for those who don't earn money from their apps and build apps just for learning or non-profit use.

### Features
- No account needed
- Uses github usage & quota
- Absolutely Free, No paid plans
- Serverless database
- In-app messaging
- In-app updates
- Cloud storage for apps
- Push messaging
- Turn your github into a backend


### Implementation
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
In module **build.gradle**
```
	dependencies {
	               implementation 'com.github.Clorabase:Clorabase:vTAG'
	}
```

[![](https://jitpack.io/v/Clorabase/Clorabase.svg)](https://jitpack.io/#Clorabase/Clorabase)


#### To generate a GitHub OAuth token
1. Goto [Create token](https://github.com/settings/tokens/new)
2. Select the 'Classic' token type and set the expiry to 'No expiration'
3. Check the following in the scope sections:
   - delete_repo
   - repo
4. Click generate.

Now you can use this token to access the console and SDK.

⚠️**NOTE: Never publish your code containing this token on GitHub, if you did, then the token will automatically deleted and your code will break**


### Documentation
- [User guide](https://clorabase-docs.netlify.app/)

### When to use this instead of Firebase
See the table below to compare Clorabase and Firebase with their features.

| Usage                     | Clorabase | Firebase |
| -----------               |-----------|----------|
| Large storage (>10GB)       | Yes       | No      |
| Large database (>512MB)         | Yes        | Yes      |
| Unlimited push notification| Yes        | No      |
| In-app messaging         | Yes        | Yes      |
| In-app update             | Yes        | Absent   |
| Authentication             | Absent | Yes|

The rule of thumb is, if you’re building a small project or dedicated mobile application, and you don’t mind the high bandwidth or database storage, Clorabase is a great place to start. If you’re looking for a more general-purpose data solution, value performance at scale and advanced querying, Firebase is going to serve your needs best.



## NEED TESTERS
**This is currently in testing phase and does not represent final quality. You may find bugs, crashes or other issues. Please report it to us.**
To participate in beta testing, email us at [participate@itsrahil.me]() or DM [@x0.rahil](https://www.instagram.com/x0.rahil/) on Instagram.
We will put your name in beta contributors and testers.

### Console
You can download the console from the assets of the latest release.
