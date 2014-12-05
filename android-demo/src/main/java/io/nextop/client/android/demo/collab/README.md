Peer-to-peer collaborative editing demo.

Notes:

- Document merging code runs on the devices. Relies on root routing to route messages to the right device.
- Documents are stored 1. as long as a device is viewing it 2. otherwise until the server times out a message. Storage uses messaging to pass off the document state.
