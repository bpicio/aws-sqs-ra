/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.bpic.aws.sqs.ra.inbound;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.*;
import com.amazonaws.regions.*;
import com.amazonaws.util.StringUtils;

import javax.jms.Queue;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import java.io.Serializable;

/**
 * {@link ActivationSpec} for SQS.
 *
 * @author Craig Andrews
 */
public class SQSJMSActivationSpec implements ActivationSpec, Serializable, AWSCredentialsProvider {

    private ResourceAdapter adapter;

    private String awsAccessKeyId;

    private String awsSecretKey;

    private String region;

    private String destination;

    private String destinationType;

    private String acknowledgeMode;

    private final AWSCredentials awsCredentials = new AWSCredentials() {
        @Override
        public String getAWSAccessKeyId() {
            if (StringUtils.isNullOrEmpty(awsAccessKeyId)) {
                throw new SdkClientException("awsAccessKeyId not set on " + this.getClass().getName());
            } else {
                return awsAccessKeyId;
            }
        }

        @Override
        public String getAWSSecretKey() {
            if (StringUtils.isNullOrEmpty(awsSecretKey)) {
                throw new SdkClientException("awsSecretKey not set on " + this.getClass().getName());
            } else {
                return awsSecretKey;
            }
        }
    };

    private final AWSCredentialsProvider awsCredentialsProvider = new AWSCredentialsProviderChain(
            new AWSStaticCredentialsProvider(awsCredentials),
            DefaultAWSCredentialsProviderChain.getInstance());

    public SQSJMSActivationSpec() {
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public void setResourceAdapter(final ResourceAdapter ra) throws ResourceException {
        // spec section 5.3.3
        if (this.adapter != null) {
            throw new ResourceException("ResourceAdapter already set");
        }
        if (!(ra instanceof SQSJMSResourceAdapter)) {
            throw new ResourceException("ResourceAdapter is not of type: " + SQSJMSResourceAdapter.class.getName());
        }
        this.adapter = ra;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        if (!Queue.class.getName().equals(destinationType)) {
            throw new InvalidPropertyException("'destinationType' must be '" + Queue.class.getName() + "'.");
        }
        try {
            SQSJMSActivation.acknowledgeModeStringToInt(acknowledgeMode);
        } catch (final IllegalArgumentException e) {
            throw new InvalidPropertyException(e.getMessage(), e);
        }
        if (StringUtils.isNullOrEmpty(getAwsRegionProvider().getRegion())) {
            throw new InvalidPropertyException("Must set the 'region' property or provide the region to use via one of the com.amazonaws.regions.DefaultAwsRegionProviderChain supported mechanisms");
        }
        try {
            getCredentials();
        } catch (final Exception e) {
            throw new InvalidPropertyException("Must set awsAccessKeyId and awsSecretKey or provide the credentials to use via one of the com.amazonaws.auth.DefaultAWSCredentialsProviderChain supported mechanisms", e);
        }
    }

    public String getAwsAccessKeyId() {
        return awsAccessKeyId;
    }

    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this.awsAccessKeyId = awsAccessKeyId;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    @Override
    public AWSCredentials getCredentials() {
        return awsCredentialsProvider.getCredentials();
    }

    public String getAcknowledgeMode() {
        return acknowledgeMode;
    }

    public void setAcknowledgeMode(final String acknowledgeMode) {
        this.acknowledgeMode = acknowledgeMode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public void refresh() {
        awsCredentialsProvider.refresh();
    }

    public AwsRegionProvider getAwsRegionProvider() {
        return new AwsRegionProviderChain(
                new AwsRegionProvider() {
                    @Override
                    public String getRegion() {
                        return StringUtils.isNullOrEmpty(region) ? Regions.getCurrentRegion().getName() : region;
                    }
                },
                new DefaultAwsRegionProviderChain());
    }
}
