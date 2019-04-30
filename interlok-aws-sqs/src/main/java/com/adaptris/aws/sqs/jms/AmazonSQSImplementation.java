/*
    Copyright 2018 Adaptris

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.adaptris.aws.sqs.jms;

import static com.adaptris.core.jms.JmsUtils.wrapJMSException;

import javax.jms.JMSException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.InputFieldDefault;
import com.adaptris.aws.AWSAuthentication;
import com.adaptris.aws.ClientConfigurationBuilder;
import com.adaptris.aws.DefaultAWSAuthentication;
import com.adaptris.aws.EndpointBuilder;
import com.adaptris.aws.sqs.SQSClientFactory;
import com.adaptris.aws.sqs.UnbufferedSQSClientFactory;
import com.adaptris.core.jms.VendorImplementationBase;
import com.adaptris.core.jms.VendorImplementationImp;
import com.adaptris.core.util.Args;
import com.adaptris.util.KeyValuePairSet;
import com.adaptris.util.NumberUtils;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * JMS VendorImplementation for Amazon SQS.
 * <p>
 * This VendorImplementation uses the Amazon SQS JMS compatibility layer. When using this class, do not use the AmazonSQS Producer
 * and Consumer classes. Use regular JMS consumers and producers instead.
 * </p>
 * 
 * @config amazon-sqs-implementation
 * @license STANDARD
 * @since 3.0.3
 */
@XStreamAlias("amazon-sqs-implementation")
public class AmazonSQSImplementation extends VendorImplementationImp {

  private static int DEFAULT_PREFETCH_COUNT = 10;

  @NotNull
  private String region;

  @Valid
  private AWSAuthentication authentication;

  @AdvancedConfig
  private Integer prefetchCount;

  @NotNull
  @AutoPopulated
  @Valid
  @InputFieldDefault(value = "UnbufferedSQSClientFactory")
  private SQSClientFactory sqsClientFactory;
  
  private transient SQSConnectionFactory connectionFactory = null;
  
  public AmazonSQSImplementation() {
    setSqsClientFactory(new UnbufferedSQSClientFactory());
  }

  @Override
  public SQSConnectionFactory createConnectionFactory() throws JMSException {
    try {
      if (connectionFactory == null) connectionFactory = build();
    }
    catch (Exception e) {
      throw wrapJMSException(e);
    }
    return connectionFactory;
  }

  protected SQSConnectionFactory build() throws Exception {
    ClientConfiguration cc = ClientConfigurationBuilder.build(new KeyValuePairSet());
    AmazonSQS sqsClient = getSqsClientFactory().createClient(authentication().getAWSCredentials(), cc, endpointBuilder());
    return new SQSConnectionFactory(newProviderConfiguration(), sqsClient);
  }

  @Override
  public boolean connectionEquals(VendorImplementationBase arg0) {
    return this == arg0;
  }

  public String getRegion() {
    return region;
  }

  /**
   * The Amazon Web Services region to use
   * 
   * @param str the region
   */
  public void setRegion(String str) {
    this.region = Args.notBlank(str, "region");
  }


  public Integer getPrefetchCount() {
    return prefetchCount;
  }

  /**
   * The maximum number of messages to retrieve from the Amazon SQS queue per request. When omitted
   * the default setting on the queue will be used.
   * 
   * @param prefetchCount
   */
  public void setPrefetchCount(Integer prefetchCount) {
    this.prefetchCount = prefetchCount;
  }

  protected int prefetchCount() {
    return NumberUtils.toIntDefaultIfNull(getPrefetchCount(), DEFAULT_PREFETCH_COUNT);
  }

  
  public AWSAuthentication getAuthentication() {
    return authentication;
  }

  /**
   * The authentication method to use
   */
  public void setAuthentication(AWSAuthentication authentication) {
    this.authentication = authentication;
  }

  public <T extends AmazonSQSImplementation> T withAuthentication(AWSAuthentication a) {
    setAuthentication(a);
    return (T) this;
  }
  
  /**
   * How to create the SQS client and set parameters.
   */
  public void setSqsClientFactory(SQSClientFactory sqsClientFactory) {
    this.sqsClientFactory = Args.notNull(sqsClientFactory, "sqsClientFactory");
  }
  
  public SQSClientFactory getSqsClientFactory() {
    return sqsClientFactory;
  }
  
  public <T extends AmazonSQSImplementation> T withClientFactory(SQSClientFactory fac) {
    setSqsClientFactory(fac);
    return (T) this;
  }
  
  protected AWSAuthentication authentication() {
    return ObjectUtils.defaultIfNull(getAuthentication(), new DefaultAWSAuthentication());
  }

  protected EndpointBuilder endpointBuilder() {
    return new RegionOnly();
  }
  
  protected ProviderConfiguration newProviderConfiguration() {
    return new ProviderConfiguration().withNumberOfMessagesToPrefetch(prefetchCount());
  }
  
  protected class RegionOnly implements EndpointBuilder {

    @Override
    public <T extends AwsClientBuilder<?, ?>> T rebuild(T builder) {
      if (StringUtils.isNotBlank(getRegion())) {
        log.trace("Setting Region to {}", getRegion());
        builder.setRegion(getRegion());
      }
      return builder;
    }
    
  }
}
