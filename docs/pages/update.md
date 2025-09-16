# In-App updates

It is common to find & fix bugs after releasing the app to production. However, you need to somehow notify the user about the new update that contains bug fixes or new features. Through this new feature, you can push updates anytime & notify users about the new update. It is best for the apps which are not on the Play Store; otherwise, you must consider using [Playcore library](https://developer.android.com/guide/playcore/in-app-updates)  

## Key features

*   In-app download
*   Simple & easy SDK
*   Various update modes (coming soon)

  

### Initializing the class

You just have to initialize the class and the rest of the library will take care. It's recommended to initialize this in **Application's** or else in the **LAUNCHER** activity `onCreate()` .  
  
Use the static `init(Context,String,String) method of class ClorabaseInAppUpdate` .

```plain
Clorabase clorabase = Clorabase.getInstance("username","YOUR_AUTH_TOKEN", "PROJECT_NAME");
clorabase.initInAppUpdate(this);
```

  
  

### Adding the app in Clorabase

For this to work, you have to first add the app to Clorabase in-app updates. You can do this easily through the console by clicking the floating button. See the picture below:  
  
![add-app-update](/docs/pictures/add-app-update.jpg)  

### Incrementing app version

When you have published your new update, increment its version from the console. To increment the version, just add the same app again with the new version code and name  
![update-info.jpg](/docs/pictures/update-info.jpg)  
  
You can also upload changelogs.md into your project updates directory using GitHub web ui. For example, if your project is `foobar` , upload the changelogs.md in `foobar/updates/com.foo.bar/`  
  

###   

_That's all you need to know about the in-app update_
