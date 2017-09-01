/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.springongcp.pubsub;

import java.io.IOException;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.config.GcpConfigProperties;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.gcp.AckMode;
import org.springframework.integration.gcp.inbound.PubSubInboundChannelAdapter;
import org.springframework.integration.gcp.outbound.PubSubMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@SpringBootApplication
public class PubsubApplication {

  private static final Log LOGGER = LogFactory.getLog(PubsubApplication.class);

  @Value("gs://jamnotifications/LockHolderKIlled_RequestProceeds.png")
  private Resource file;

  @Autowired
  private GcpConfigProperties configProperties;

  public static void main(String[] args) throws IOException {
    SpringApplication.run(PubsubApplication.class, args);
  }

  // Inbound channel adapter.

  @Bean
  public MessageChannel pubsubInputChannel() {
    return new PublishSubscribeChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter messageChannelAdapter(
      @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
      SubscriberFactory subscriberFactory) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(subscriberFactory, "messages");
    adapter.setOutputChannel(inputChannel);
    adapter.setAckMode(AckMode.MANUAL);

    return adapter;
  }

  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler messageReceiver1() {
    return message -> {
      LOGGER.info("Message arrived! Payload: "
          + ((ByteString) message.getPayload()).toStringUtf8());
      AckReplyConsumer consumer =
          (AckReplyConsumer) message.getHeaders().get(GcpHeaders.ACKNOWLEDGEMENT);
      consumer.ack();
    };
  }


  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler messageReceiver2() {
    return message -> {
      LOGGER.info("Message also arrived here! Payload: "
          + ((ByteString) message.getPayload()).toStringUtf8());
      AckReplyConsumer consumer =
          (AckReplyConsumer) message.getHeaders().get(GcpHeaders.ACKNOWLEDGEMENT);
      consumer.ack();
    };
  }

  // Outbound channel adapter

  @Bean
  @ServiceActivator(inputChannel = "pubsubOutputChannel")
  public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
    PubSubMessageHandler outboundAdapter = new PubSubMessageHandler(pubsubTemplate);
    outboundAdapter.setTopic("test");
    return outboundAdapter;
  }

  @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
  public interface PubsubOutboundGateway {

    void sendToPubsub(String text);
  }

}
