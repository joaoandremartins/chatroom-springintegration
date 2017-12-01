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

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.storage.Storage;
import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.support.ValueExpression;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.gcp.pubsub.AckMode;
import org.springframework.integration.gcp.pubsub.inbound.PubSubInboundChannelAdapter;
import org.springframework.integration.gcp.pubsub.outbound.PubSubMessageHandler;
import org.springframework.integration.gcp.storage.GCSSessionFactory;
import org.springframework.integration.gcp.storage.inbound.StorageInboundChannelAdapter;
import org.springframework.integration.gcp.storage.outbound.StorageOutboundChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class PubsubApplication {

  private static final Log LOGGER = LogFactory.getLog(PubsubApplication.class);

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
      PubSubTemplate pubSubTemplate) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, "messages");
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
    return new PubSubMessageHandler(pubsubTemplate, "test");
  }

  @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
  public interface PubsubOutboundGateway {

    void sendToPubsub(String text);
  }


  @Bean
  @InboundChannelAdapter(channel = "blobChannel", poller = @Poller(fixedDelay = "5"))
  public MessageSource<InputStream> inboundAdapter(Storage gcs) {
    StorageInboundChannelAdapter adapter =
            new StorageInboundChannelAdapter(new RemoteFileTemplate<>(new GCSSessionFactory(gcs)));
    adapter.setRemoteDirectory("springone-houses");
    adapter.setFilter(new AcceptOnceFileListFilter<>());
    return adapter;
  }

  @Bean
  @ServiceActivator(inputChannel = "blobChannel", poller = @Poller(fixedDelay = "10"))
  public MessageHandler handleFiles() {
    return message -> LOGGER.info(message.getHeaders().get(FileHeaders.REMOTE_FILE));
  }

  @Bean
  public MessageChannel blobChannel() {
    return new QueueChannel();
  }

  @Bean
  @ServiceActivator(inputChannel = "writeFiles")
  public MessageHandler outboundChannelAdapter(Storage gcs) {
    StorageOutboundChannelAdapter outboundChannelAdapter =
            new StorageOutboundChannelAdapter(new GCSSessionFactory(gcs));
    outboundChannelAdapter.setRemoteDirectoryExpression(new ValueExpression<>("springintegrationz"));

    return outboundChannelAdapter;
  }

  @MessagingGateway(defaultRequestChannel = "writeFiles")
  public interface SIFileGateway {

    void sendFileToGCS(File file);
  }
}
