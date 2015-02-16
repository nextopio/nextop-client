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

Be a [![star](docs/assets/star-16.png)](https://github.com/nextopio/nextop-client/stargazers)


## Android

We've rebuilt the Android network stack to focus on image upload and download, and
improving transfer of all requests in all network conditions.


Gradle dependency:

```
compile group: 'io.nextop', name: 'android', version: '0.1.2'
```

Maven dependency:

```xml
<dependency>
  <groupId>io.nextop</groupId>
  <artifactId>android</artifactId>
  <version>0.1.2</version>
</dependency>
```

### Android Benchmarks

(coming soon)

### Android Demos

| Demo       | Install                                                                                                                                | Objective                                         | Code                                                                                                                                                        |
|------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| dılɟ       | [![Android App on Play](docs/assets/en_app_rgb_wo_60-32.png)](https://play.google.com/store/apps/details?id=io.nextop.demo.flip)       | Ridiculously fast image loading on mobile         | [App](https://github.com/nextopio/nextop-client/tree/master/android-demo-flip), [Server](https://github.com/nextopio/nextop-client/tree/master/backend-demo-flip)     |


### Android Blog Posts

| Date       | Post                                                                                                                                              |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| 02.13.15   | [Binding Rx subscriptions to view lifecycles, and using them to optimize your network traffic](docs/02.13.15_SUBSCRIPTIONS_NETWORKING_VIEWS.md)   |


# About Us

Nextop is an open source project sponsored by [Nextop Inc](http://nextop.io). We build client SDKs and a service to power them.

Get Support <<support@nextop.io>>

Twitter [@nextopio](https://twitter.com/nextopio)
