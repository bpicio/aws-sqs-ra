package io.bpic.aws.sqs.ra.inbound;

import javax.jms.*;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: 12/23/19 5:07 PM.
 *
 * @author palermo
 */
public class SQSJMSResourceAdapter implements ResourceAdapter {

    private static final Logger logger = Logger.getLogger("io.bpic.aws.sqs.ra");

    private final Map<ActivationSpec, SQSJMSActivation> activations;

    private SQSJMSActivationSpec sqsSpec;

    public SQSJMSResourceAdapter() {
        this.activations = new ConcurrentHashMap<>();
    }

    @Override
    public void start(BootstrapContext ctx) {
        logger.info("Amazon SQS Resource Adapter Started...");
    }

    @Override
    public void stop() {
        logger.info("Amazon SQS Resource Adapter Stopped!");
        for (final SQSJMSActivation value : activations.values()) {
            try {
                value.close();
            }
            catch (JMSException e) {
                logger.log(Level.SEVERE, "Failed to close connection", e);
            }
        }
        activations.clear();
    }

    @Override
    public void endpointActivation(final MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {this.sqsSpec = (SQSJMSActivationSpec) spec;
        SQSJMSActivation activation = new SQSJMSActivation(endpointFactory, sqsSpec);
        activations.put(spec, activation);
        activation.start();
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        SQSJMSActivation activation = activations.remove(spec);
        if (activation != null) {
            try {
                activation.close();
            } catch (JMSException e) {
                logger.log(Level.SEVERE, "Failed to close connection", e);
            }
        }
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        throw new NotSupportedException("Not supported!");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SQSJMSResourceAdapter that = (SQSJMSResourceAdapter) o;
        return activations.equals(that.activations) &&
                sqsSpec.equals(that.sqsSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activations, sqsSpec);
    }
}
