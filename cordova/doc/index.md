# io.nextop.cordova

This plugin replaces `window.XMLHttpRequest` with an implementation backed
by Nextop, the mobile CDN. Nextop mitigates network issues
and speeds up all requests. Read more about Nextop at http://nextop.io.

## Installation

```
cordova plugin add io.nextop.cordova
```

## Properties

In addition to the full set of properties described at
 [XMLHttpRequest](https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest):

- XMLHttpRequest.nextopVersion
- XMLHttpRequest.download

## Supported Platforms

- Android

## Quirks

- `async=false` is implemented with the legacy XMLHttpRequest.
  In the future there might be some intelligent retry,
  but currently it is just a passthrough.

## Access Key

Without an access key (or when the Nextop service is down) this plugin is a reliable HTTP client.
With an access key, this plugin optimizes for bad networks beyond what HTTP can deliver.

Get an access key at http://nextop.io.

Add the access key in your `config.xml` as a preference.

```
<preference name="NextopAccessKey" value="$access-key" />
```

## Support

| Email          | support@nextop.io                  |
|----------------|------------------------------------|
| Live Chat      | [#nextop](https://www.hipchat.com/gebRowlQF)  |
| SMS            | +1-650-862-1946                    |

