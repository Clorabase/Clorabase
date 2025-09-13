# In-app messaging
Clorabase In-App Messaging helps you engage your app's active users by sending them targeted, contextual messages that encourage them to use key app features. For example, you could send an in-app message to get users to subscribe, watch a video, complete a level, or buy an item.

### Key features
- Unlimited In-app messaging
- Simple & lightweight SDK
- Remote code execution (coming soon)


## Initializing
To use this feature you have to initialize it first. It is recommended to initialize it in **Application's onCreate()**, like this:
```java
public class MainActivity extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Clorabase clorabase = Clorabase.getInstance("YOUR_AUTH_TOKEN", "PROJECT_NAME");
        clorabase.initInAppMessaging(this,"channel");
    }
}
```


## Sending In-app message
To send an In-app message, In the console go to **In-app messaging** option and fill in the blanks of the message. Click the **send** button to send the notification.

![in-app.png](inapp.png)

Here, **Channel** is the name of the messaging channel on which you want to send an In-app message. That app must have been initialized through the SDK method.

*That's all that you need to know about in-app messaging*
