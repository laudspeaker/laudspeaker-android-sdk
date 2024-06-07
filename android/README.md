# Laudspeaker Android SDK Integration
## Setup
To setup Laudspeaker Android SDK perform these steps:
- Add laudspeaker dependency:
```
implementation "........."
```
- Perform Grandle sync
- Add required permissions to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
- setup firebase using instructions mentioned in [Firebase setup guide](https://firebase.google.com/docs/android/setup)
## Example
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  // initialize firebase integration
  FirebaseApp.initializeApp(this);

  ...

  // create instance
  LaudspeakerAndroid laudspeakerAndroid = new LaudspeakerAndroid(getPreferences(MODE_PRIVATE));

  // describe customer's unique properties for identification
  Map<String, Object> uniquePropertiesMap = new HashMap<>();
  uniquePropertiesMap.put("email", "email@gmail.com");

  // implement onConnect listener
  ConnectListener connectListener = () -> {
    // identify the customer
    laudspeakerAndroid.identify(uniquePropertiesMap);
    // retrive and assign customer's FCM token
    laudspeakerAndroid.sendFCMToken();
  };

  laudspeakerAndroid.onConnected(connectListener);

  // connect to laudspeaker services using your api token and host
  try {
    laudspeakerAndroid.connect("YOUR_API_KEY", "https://laudspeaker.com");
  } catch (URISyntaxException e) {
    throw new RuntimeException(e);
  }
}
```