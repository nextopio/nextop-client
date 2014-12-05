Projects we use:

- RxCpp
- RxJava
- ReactiveCocoa
- Djinni


Properties (notes)

- reactor: all receives are processed on a scheduler. 
- chained+cached loading: identical messages to the same GET path will 1. attach to pending messages 2. consult the network cache (pending time to live). The client correctly parses cacheable and time to live headers.
- (proxy) root routing: paths with variables are routed and ordered by the first (root) variable. e.g. all messages to /form/${id}/... will be sent to the same client in order (for all paths that the client can handle). The routing may change depending on load balancing (but will always wait for completion on the client before changing the route).
- (proxy) load balancing: subscription to client receives may be completed, then after completion, pending messages routed to a new client

