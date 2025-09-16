# In-app messaging

Clorabase In-App Messaging helps you engage your app's active users by sending them in-app messages that encourage them to use key app features. For example, you could send an in-app message to get users to subscribe, watch a video, complete a level, or buy an item.  

### Key features

*   Unlimited In-app messaging
*   Simple & lightweight SDK
*   Contextual and Conditional messaging (coming soon)

  

## Initializing

To use this feature, you have to initialise it first. You can initialise it anytime you want to display the message but it is recommended to initialise it in the **Application's onCreate()**, like this:

```java
public class MainActivity extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Clorabase clorabase = Clorabase.getInstance("USERNAME","YOUR_AUTH_TOKEN", "PROJECT_NAME");
        clorabase.initInAppMessaging(this);
    }
}
```

Once it is initialised, it will start checking for any pending messages from the message queue. If there is any, it will display it, and the message will be removed from the queue  
  

## Sending In-app message

To send an In-app message, In the console, go to **In-app messaging** option and fill in the blanks of the message. Click the **send** button to send the notification.  
  
![In-app](/docs/pictures/in-app.jpg)  
  
_That's all that you need to know about in-app messaging_
