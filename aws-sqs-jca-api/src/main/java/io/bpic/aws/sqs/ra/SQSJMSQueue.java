package io.bpic.aws.sqs.ra;

import javax.jms.JMSException;
import javax.jms.Queue;

/**
 * Created: 12/26/19 1:30 PM.
 *
 * @author palermo
 */
public class SQSJMSQueue implements Queue {

    private String queueName;

    public SQSJMSQueue() {
    }

    public SQSJMSQueue(String queueName) {
        this.queueName = queueName;
    }

    @Override
    public String getQueueName() throws JMSException {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}
