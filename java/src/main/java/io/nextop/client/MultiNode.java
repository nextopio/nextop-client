package io.nextop.client;


// if no downstreams are active, holds until one is
// do not determine an active node until all downstream are started
// (this handles the case of a downstream calling upstream during start?)
public class MultiNode extends AbstractMessageControlNode {


    // connects to each Transport

    // can make one transport unavailable and transfer messages to another transport
    // middle man to handle nack issues (out of order issues)
    // where a transport committed a message but is still waiting on the reply

    MultiNode(WeightedDownstream ... downstreams) {

    }



    // the strategy should be deterministic, so the same result is returned for each call with the same args
    // (threading) the optimization strategy must be set either before init is called, or on the node handler
    void setOptimizationStrategy(OptimizationStrategy s) {

    }





    void onActive(boolean active, MessageControlMetrics metrics) {

    }
    void onTransfer(MessageControlState mcs) {

    }
    void onMessageControl(MessageControl mc) {

    }






    public static final class WeightedDownstream {
        public final MessageControlNode node;

        public final MessageControlMetrics prior;


        public WeightedDownstream(MessageControlNode node) {
            this(node, new MessageControlMetrics());
        }

        public WeightedDownstream(MessageControlNode node, MessageControlMetrics prior) {
            this.node = node;
            this.prior = prior;
        }
    }

    // func2<Downstream, Quality, Float>
    //

    // opt target = (a, b, c) . (quality, cost, battery)




    interface OptimizationStrategy {
        int rank(MessageControlMetrics apriori, MessageControlMetrics current);
    }



}
