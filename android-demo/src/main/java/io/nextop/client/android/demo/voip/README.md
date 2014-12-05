Peer-to-peer VOIP demo. Input from the client mixed on a server and then sent back to the clients for playback.

Notes:

- Because Nextop uses TCP, voice data uses TCP. However, using client.cancel on the prior message may discard backed up messages like UDP.