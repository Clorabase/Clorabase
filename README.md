# Clorabase
> A account-free backend for android apps.



### What it is
Clorabase is a free and backend alternative for android apps. Clorabase is mostly made for apps which don't read or write data everytime, instead they syncs the local database with the server rearly. Like firebase, clorabase also sync data in realtime but with different approch. It uses [CloremDB](https://github.com/ErrorxCode/Clorem) as its database. User read & write on its local database and can sync it with the server whenever he/she wants. If the app depends totally on the data in the database, basically if the data is changed in the database by other user and you are dependent on that data then you can always get the data from database while initializing, else it is also recommanded to fetch data only when needed.

### Features
- No account needed
- Unlimited usage & quota
- Absolutly Free, No paid plans
- Online database
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
	        implementation 'com.github.ErrorxCode:Clorabase:v1.5-beta-3'
	}
```

#### To get console
Goto latest release and download "Clorabase-beta.apk" from the assets.

### When to use this instead of firebase
Clorabase and firebase are two most popular SaaS for android apps. One can deside what to use ant at what time through the below table.

| Usage                     | Clorabase | Firebase |
| -----------               |-----------|----------|
| Large storage (>10GB)       | Yes       | No      |
| Large database (>1GB)         | Yes        | No      |
| Unlimited push notification| Yes        | No      |
| Advance messaging         | No        | Yes      |
| Simple In-App messaging      | Yes        | No      |
| In-app update             | Yes        | Absent   |
| Authentication             | Absent | Yes|


#### Example project
[Clorabase start-up]()

## BETA TESTING
**This is currently in beta testing and does not represent final quality. You may find bugs,crashes or other issues. Please report it to us.**
To participate in beta testing, email us at [inboxrahil@xcoder.tk]() or DM [@x__coder__x](https://www.instagram.com/x__coder__x/) on instagram.
We will put your name in beta contributors and testers.

**NOTE :** You will get console after you request for testing.
