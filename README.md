# Clorabase
> A account-free backend for android apps.



### What it is
Clorabase is a free and backend alternative (BaaS) for android apps. Clorabase is mostly made for small apps which has small backend/server/database requirement. If your app scale large or you need more database storage or bandwidth, clorabase may not serve you. You should consider using [Firebase](https://firebase.google.com). Clorabase AIMS to provide a money-free production ready backend for building android apps as hobby or start-up. It is for those who don't earn money from their apps and build apps just for learning or non-profit use.

### Features
- No account needed
- Unlimited usage & quota
- Absolutly Free, No paid plans
- Serverless database
- In-app messging
- In-app updates
- Cloud storage for apps
- Push messaging


### Implimentation
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
	               implementation 'com.github.Clorabase:Clorabase:Tag'
	}
```
[![](https://jitpack.io/v/Clorabase/Clorabase.svg)](https://jitpack.io/#Clorabase/Clorabase)

### Documentation
- [User guide](https://docs.clorabase.tk)

### When to use this instead of firebase
See the table below to campare clorabase and firebase with their features.

| Usage                     | Clorabase | Firebase |
| -----------               |-----------|----------|
| Large storage (>10GB)       | Yes       | No      |
| Large database (>512MB)         | Yes        | Yes      |
| Unlimited push notification| Yes        | No      |
| In-app messaging         | Yes        | Yes      |
| In-app update             | Yes        | Absent   |
| Authentication             | Absent | Yes|

The rule of thumb is, if you’re building a small project or dedicated mobile application, and you don’t mind the high bandwidth or database storage, Clorabase is a great place to start. If you’re looking for a more general-purpose data solution, value performance at scale and advanced querying, Firebase is going to serve your needs best.

#### Example project
[Clorabase start-up]()

## BETA TESTING
**This is currently in beta testing and does not represent final quality. You may find bugs,crashes or other issues. Please report it to us.**
To participate in beta testing, email us at [participate@rahilxcoder.tk]() or DM [@x0.rahil](https://www.instagram.com/x0.rahil/) on instagram.
We will put your name in beta contributors and testers.

### Console
You can download the console from the accests of the latest release.
