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


        public WeightedDownstream(MessageControlNode node, int preference) {
            this(node, preferencePrior(preference));
        }

        public WeightedDownstream(MessageControlNode node, MessageControlMetrics prior) {
            this.node = node;
            this.prior = prior;
        }


        private static MessageControlMetrics preferencePrior(int preference) {
            MessageControlMetrics prior = new MessageControlMetrics();
            prior.preference = preference;
            return prior;
        }
    }

    // func2<Downstream, Quality, Float>
    //

    // opt target = (a, b, c) . (quality, cost, battery)




    public interface OptimizationStrategy {
        float rank(MessageControlMetrics prior, MessageControlMetrics current);
    }

    public static class DotOptimizationStrategy implements OptimizationStrategy {
        MessageControlMetrics weight;
        float alpha;

        public DotOptimizationStrategy(MessageControlMetrics weight, float alpha) {
            this.weight = weight;
            this.alpha = alpha;
        }


        @Override
        public float rank(MessageControlMetrics prior, MessageControlMetrics current) {
            return project(prior.battery, current.battery, alpha)
                    + project(prior.cost, current.cost, alpha)
                    + project(prior.quality, current.quality, alpha)
                    + project(prior.preference, current.preference, alpha);
        }


        private static float project(int a, int b, float alpha) {
            return alpha * fill(a, b) + (1 - alpha) * fill(b, a);
        }
        private static int fill(int a, int b) {
            return 0 < a ? a : b;
        }
    }



}
