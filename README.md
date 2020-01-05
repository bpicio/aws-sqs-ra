# Amazon SQS JMS Resource Adapter

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.bpic.aws/aws-sqs-ra/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.bpic.aws/aws-sqs-ra)

## Getting Started

### Installation
For Wildfly, copy aws-sqs-rar-[VERSION HERE].rar to your application server deployment folder. In standalone it should be:
```
$WILDFLY_HOME/standalone/deployments
```

Either reference the resource adapter on your MDB using:
``` java
@ResourceAdapter("aws-sqs-rar-[VERSION HERE].rar")
```

Or by setting as the default adapter on standalone-full.xml:

```xml
<subsystem xmlns="urn:jboss:domain:ejb3:6.0">
    ...
    <mdb>
        <resource-adapter-ref resource-adapter-name="${ejb.resource-adapter-name:activemq-ra.rar}"/>
        ...
    </mdb>
    ...
</subsystem>
``` 
Then create a queue:

```xml
<subsystem xmlns="urn:jboss:domain:resource-adapters:5.0">
    <resource-adapters>
        <resource-adapter id="aws-sqs-rar-[VERSION HERE].rar">
            <archive>
                aws-sqs-rar-${project.version}.rar
            </archive>
            <transaction-support>NoTransaction</transaction-support>
            <admin-objects>
                <admin-object class-name="io.bpic.aws.sqs.ra.SQSJMSQueue" jndi-name="java:jboss/sqs/queue/QueueName" pool-name="QueuePool">
                    <config-property name="queueName">[QUEUE NAME]</config-property>
                </admin-object>
            </admin-objects>
        </resource-adapter>
    </resource-adapters>
</subsystem>
```

### Inbound MDB
 To receive messages you must implement the MessageListener interface. 
 ```java
public class ReceiveSQSMDB implements MessageListener {
    
} 
 ```
 
 Also you must set the ActivationConfigProperty values suitable for your MDB. 
 
 Valid properties are below.
 
 |Config Property Name | Type | Default | Notes
 |---------------------|------|---------|------
 |awsAccessKeyId | String | None | Must be set to the access key of your AWS account (Optional)
 |awsSecretKey | String | None | Must be set to the secret key of your AWS account (Optional)
 |destination | String | None | JNDI queue name
 |region | String | None | Must be set to the AWS region name of your queue. (Optional) 
 
 Your MDB should contain one method annotated with `@OnMessage`
 
 A full skeleton MDB is shown below
 ```java
 @MessageDriven(activationConfig = {
     @ActivationConfigProperty(propertyName = "awsAccessKeyId", propertyValue = "${ENV=accessKey}"),
     @ActivationConfigProperty(propertyName = "awsSecretKey", propertyValue = "${ENV=secretKey}"),
     @ActivationConfigProperty(propertyName = "destination", propertyValue = "${ENV=destination}"),   
     @ActivationConfigProperty(propertyName = "region", propertyValue = "eu-west-2")    
 })
@ResourceAdapter("aws-sqs-rar-[VERSION HERE].rar")
 public class ReceiveSQSMDB implements MessageListener {
 
     @OnMessage
     public void receiveMessage(Message message) {
         System.out.println("Got message " + message.getBody());
     }
 }
 ```

## License

This project is licensed under the GNU AGPLv3 License - see the [LICENSE.md](LICENSE.md) file for details
