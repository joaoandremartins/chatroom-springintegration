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
import com.google.springongcp.model.LoggableMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SpringBootApplication
@EnableConfigurationProperties(SampleConfig.class)
public class PubsubApplication {

  private static final Log LOGGER = LogFactory.getLog(PubsubApplication.class);

  public static void main(String[] args) throws IOException {
    SpringApplication.run(PubsubApplication.class, args);

//    ConsoleHandler consoleHandler = new ConsoleHandler();
//    consoleHandler.setLevel(Level.ALL);
//    consoleHandler.setFormatter(new SimpleFormatter());
//
//    Logger logger = Logger.getLogger("com.google.api.client");
//    logger.setLevel(Level.ALL);
//    logger.addHandler(consoleHandler);
//
//    Logger lh = Logger.getLogger("httpclient.wire.header");
//    lh.setLevel(Level.ALL);
//    lh.addHandler(consoleHandler);
//
//    Logger lc = Logger.getLogger("httpclient.wire.content");
//    lc.setLevel(Level.ALL);
//    lc.addHandler(consoleHandler);
//
//    Logger gl = Logger.getLogger("io.grpc");
//    gl.setLevel(Level.FINE);
//    gl.addHandler(consoleHandler);
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
      LOGGER.info("Message arrived! Payload: " + message.getPayload());
      AckReplyConsumer consumer =
          (AckReplyConsumer) message.getHeaders().get(GcpHeaders.ACKNOWLEDGEMENT);
      consumer.ack();
    };
  }


  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler messageReceiver2() {
    return message -> {
      LOGGER.info("Message also arrived here! Payload: " + message.getPayload());
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

    void sendToPubsub(byte[] bytes);

    void sendToPubsub(LoggableMessage loggableMessage);
  }


//  @Bean
//  @InboundChannelAdapter(channel = "blobChannel", poller = @Poller(fixedDelay = "5"))
//  public MessageSource<InputStream> inboundAdapter(Storage gcs) {
//    GcsStreamingMessageSource adapter =
//            new GcsStreamingMessageSource(new GcsRemoteFileTemplate(new GcsSessionFactory(gcs)));
//    adapter.setRemoteDirectory("springintegrationz");
//    adapter.setFilter(new AcceptOnceFileListFilter<>());
//    return adapter;
//  }
//
//  @Bean
//  @ServiceActivator(inputChannel = "blobChannel", poller = @Poller(fixedDelay = "10"))
//  public MessageHandler handleFiles() {
//    return message -> LOGGER.info(message.getHeaders().get(FileHeaders.REMOTE_FILE));
//  }
//
//  @Bean
//  public MessageChannel blobChannel() {
//    return new QueueChannel();
//  }
//
//  @Bean
//  @ServiceActivator(inputChannel = "writeFiles")
//  public MessageHandler outboundChannelAdapter(Storage gcs) {
//    GcsMessageHandler outboundChannelAdapter = new GcsMessageHandler(new GcsSessionFactory(gcs));
//    outboundChannelAdapter.setRemoteDirectoryExpression(new ValueExpression<>("springintegrationz"));
//
//    return outboundChannelAdapter;
//  }
//
//  @MessagingGateway(defaultRequestChannel = "writeFiles")
//  public interface SIFileGateway {
//
//    void sendFileToGCS(File file);
//  }
//
//  @Bean
//  @InboundChannelAdapter(channel = "synchronizer", poller = @Poller(fixedDelay = "5000"))
//  public MessageSource<File> synchronizerAdapter(Storage gcs) {
//    GcsInboundFileSynchronizer synchronizer = new GcsInboundFileSynchronizer(gcs);
//    synchronizer.setRemoteDirectory("springintegrationz");
//
//    GcsInboundFileSynchronizingMessageSource synchAdapter =
//            new GcsInboundFileSynchronizingMessageSource(synchronizer);
//    synchAdapter.setLocalDirectory(new File("springintegrationz"));
//    synchAdapter.setAutoCreateLocalDirectory(true);
//
//    return synchAdapter;
//  }
//
//  @Bean
//  @ServiceActivator(inputChannel = "synchronizer")
//  public MessageHandler handleSynchronizerFiles() {
//    return message -> LOGGER.info("Synchronized file " + ((File) message.getPayload()).getName());
//  }
}
