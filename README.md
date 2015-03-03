# Nextop Mobile CDN

Nextop is rethinking a CDN for mobile from the ground up. Mobile devices operate in high latency and
 uncertain networks, and Nextop is able to optimize traffic to get content displayed to
 users >2x faster than leading CDNs, while never losing a transaction.

The core technology is a client SDK that replaces or plugs into your existing network library,
and a proxy server that translates between the SDK and your existing HTTP backend.

With this setup, for example, Nextop can load image previews and
blocking API calls before loading everything else for a view. As a developer you
 write network code and server code as you normally would, and Nextop takes care
 of optimizing all requests to get content displayed to the user faster.

Nextop is a cross-platform project, with Android, iOS, and webviews as top priorities.

[Talk To Us](https://github.com/nextopio/nextop-client/issues)

[Be a ![star](docs/assets/star-16.png)](https://github.com/nextopio/nextop-client/stargazers)


## Android

We've rebuilt the Android network stack to focus on image upload and download, and
improving transfer of all requests in all network conditions.


| Current Version |
|-----------------|
| 0.1.3           |

| Module              | Android Versions Supported   | Notes                                                             |
|---------------------|------------------------------|-------------------------------------------------------------------|
| `android`           | 10+                          |                                                                   |
| `android-v15`       | 15+                          | Includes all of `android` plus fragments                          |

Gradle dependency (AAR):

```
compile 'io.nextop:${MODULE}:+'
```

Maven dependency (AAR). This is for [android-maven-plugin](https://code.google.com/p/maven-android-plugin/) users.
As of this writing, Eclipse does not support AAR. [See special instructions for Eclipse users here](docs/eclipse.md).

```xml
<dependency>
  <groupId>io.nextop</groupId>
  <artifactId>${MODULE}</artifactId>
  <version>LATEST</version>
  <type>aar</type>
</dependency>
```

Maven dependency ([JARs - see notes](docs/eclipse.md)):

```xml
<dependency>
  <groupId>io.nextop</groupId>
  <artifactId>${MODULE}</artifactId>
  <version>LATEST</version>
  <type>jar</type>
</dependency>
```

Direct downloads to put in `libs/` ([JARs - see notes](docs/eclipse.md)):
```
http://search.maven.org/remotecontent?filepath=io/nextop/android-v15/${CURRENT_VERSION}/android-v15-${CURRENT_VERSION}.jar
http://search.maven.org/remotecontent?filepath=io/nextop/android/${CURRENT_VERSION}/android-${CURRENT_VERSION}.jar
http://search.maven.org/remotecontent?filepath=io/nextop/java-common/${CURRENT_VERSION}/java-common-${CURRENT_VERSION}-all.jar
```


### Android Demos

| Demo       | Install                                                                                                                                | Objective                                         | Code                                                                                                                                                                       |
|------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| dılɟ       | [![Play](docs/assets/en_app_rgb_wo_60-32.png)](https://play.google.com/store/apps/details?id=io.nextop.demo.flip)                      | Ridiculously fast image loading on mobile         | [App](https://github.com/nextopio/nextop-client/tree/master/android-demo-flip), [Server](https://github.com/nextopio/nextop-client/tree/master/backend-demo-flip)          |


### Android Blog Posts

| Date       | Post                                                                                                                                              |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| 03.02.15   | [Faster Image Loading on Android is now a one-liner](03.02.2015_FAST_IMAGE_LOADING_EASY_INTEGRATION.md)                                           |
| 02.23.15   | [![profile](docs/assets/profile_32.png) How do mobile app teams QA test poor cellphone connections?](http://qr.ae/EMeBB)                          |
| 02.13.15   | [Binding Rx subscriptions to view lifecycles, and using them to optimize your network traffic](docs/02.13.15_SUBSCRIPTIONS_NETWORKING_VIEWS.md)   |

## Cordova

See [io.nextop.cordova on the Cordova Plugins Registry](http://plugins.cordova.io/#/package/io.nextop.cordova).

### Cordova Blog Posts

| Date       | Post                                                                                                                                              |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| 02.16.15   | [![cordova](docs/assets/cordova_24.png) Things you can do with a Custom XMLHttpRequest](docs/02.16.2015_CUSTOM_XMLHTTPREQUEST.md)                 |


# About Us

Nextop is an open source project sponsored by [Nextop Inc](http://nextop.io). We build client SDKs and a service to power them.

Get Support <<support@nextop.io>>

Twitter [@nextopio](https://twitter.com/nextopio)
