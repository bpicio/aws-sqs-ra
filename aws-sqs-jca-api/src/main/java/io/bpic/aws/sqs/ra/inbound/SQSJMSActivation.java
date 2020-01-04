package io.bpic.aws.sqs.ra.inbound;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import java.lang.IllegalStateException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: 12/23/19 8:13 PM.
 *
 * @author palermo
 */
public class SQSJMSActivation {

    private static final Logger logger = Logger.getLogger(SQSJMSActivation.class.getName());

    private MessageEndpointFactory endpointFactory;

    private SQSJMSActivationSpec spec;

    /** The message endpoint factory */
    private SQSConnection connection;

    private static final Method ON_MESSAGE;

    static {
        try {
            ON_MESSAGE = MessageListener.class.getMethod("onMessage", Message.class);
        }
        catch (final NoSuchMethodException | SecurityException e) {
            // this should never happen
            throw new ExceptionInInitializerError(e);
        }
    }

    public SQSJMSActivation(MessageEndpointFactory endpointFactory, SQSJMSActivationSpec spec) {
        this.endpointFactory = endpointFactory;
        this.spec = spec;
    }

    public void start() throws ResourceException {
        logger.info("Starting activation...");
        final String region = spec.getAwsRegionProvider().getRegion();
        if (region == null) {
            throw new IllegalStateException("No region set. Please set a region or provide one using a method supported by com.amazonaws.regions.DefaultAwsRegionProviderChain");
        }
        Regions regions = Regions.fromName(region);
        final SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                AmazonSQSClientBuilder.standard()
                        .withRegion(regions)
                        .withCredentials(spec)
        );

        Queue sqsJmsQueue = getSQSJMSQueue(spec);
        try {
            this.connection = connectionFactory.createConnection();
            final QueueSession session = connection.createQueueSession(false, acknowledgeModeStringToInt(spec.getAcknowledgeMode()));

            final Queue queue;
            try {
                queue = session.createQueue(sqsJmsQueue.getQueueName());
            }
            catch (final InvalidDestinationException | QueueDoesNotExistException e) {
                throw new ResourceException("Queue with name '" + sqsJmsQueue.getQueueName() + "' does not exist", e);
            }

            final MessageConsumer messageConsumer = session.createConsumer(queue);
            messageConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    MessageEndpoint messageEndpoint = null;
                    try {
                        messageEndpoint = endpointFactory.createEndpoint(null);
                        messageEndpoint.beforeDelivery(ON_MESSAGE);
                        if (message != null) {
                            ON_MESSAGE.invoke(messageEndpoint, message);
                        }
                        messageEndpoint.afterDelivery();
                    } catch (final NoSuchMethodException | ResourceException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        logger.log(Level.SEVERE, null, e);
                    } finally {
                        if (messageEndpoint != null) {
                            messageEndpoint.release();
                        }
                    }
                }
            });
            connection.start();

        } catch (ResourceException | JMSException e) {
            throw new ResourceException(e);
        }
    }

    static int acknowledgeModeStringToInt(final String acknowledgeMode) {
        if ("auto-acknowledge".equalsIgnoreCase(acknowledgeMode)) {
            return Session.AUTO_ACKNOWLEDGE;
        }
        else if ("client-acknowledge".equalsIgnoreCase(acknowledgeMode)) {
            return Session.CLIENT_ACKNOWLEDGE;
        }
        else if ("dups-ok-acknowledge".equalsIgnoreCase(acknowledgeMode)) {
            return Session.DUPS_OK_ACKNOWLEDGE;
        }
        else {
            throw new IllegalArgumentException("valid acknowledgeModes are: auto-acknowledge, client-acknowledge, and dups-ok-acknowledge");
        }
    }

    public void close() throws JMSException {
        if (connection != null)
            connection.close();
    }

    private Queue getSQSJMSQueue(final SQSJMSActivationSpec sqsSpec) throws ResourceException {
        try {
            InitialContext ctx = new InitialContext();
            return (Queue) ctx.lookup(sqsSpec.getDestination());
        }
        catch (final NamingException e) {
            throw new ResourceException("JNDI lookup failed for "
                    + sqsSpec.getDestination(), e);
        }
    }
}
