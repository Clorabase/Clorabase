# Clorabase
> A account-free backend for android apps.



### What it is
Clorabase is a free and backend alternative for android apps. Clorabase is mostly made for apps
which hardly requires online database and other android backend features. Although, this can also be used for the hybrid apps which often uses database and other features but considering firebase for that will be more beneficial. As the limits or quota are based on the assumption that the app will not fully depend on the server. The biggest advantage of using this instead of firebase for small apps is that you don't have to build an account in order to use clorabase. Yes, this service does not need account.

### Features
- No account needed
- Much usage quota than others.
- Easy,Simple and lightweight
- Free, No paid plans
- Almost all engage features


### Implimentation
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
	        implementation 'com.github.ErrorxCode:Clorabase:1.0-alpha'
	}
```

### When to use this instead of firebase
Clorabase is made for apps that hardly or rearely communicates to the backend. Apps which rely totally on backend-database should not use this, instead they must go for [firebase](https://firebase.google.com/) because clorabase only offers 10MB of data to be stored in database. If your app is native application and only store some data on database then you should consider using this. See the table below to understand more crearly.

| Usage                     | Clorabase | Firebase |
| -----------               |-----------|----------|
| Large storage (>1GB)       | Yes       | No      |
| Large data (>10MB)         | No        | Yes      |
| Unlimited push notification| Yes        | No      |
| Advance messaging         | No        | Yes      |
| Easy In-App messaging      | Yes        | No      |
| In-app update             | Yes        | Absent   |
| Authentication             | Absent | Yes|



## Documentation



#### Example project
[Clorabase start-up]()
