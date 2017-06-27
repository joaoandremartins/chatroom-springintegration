package com.google.springongcp.pubsub;

import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.InstantiatingExecutorProvider;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.spi.v1.AckReplyConsumer;
import com.google.protobuf.ByteString;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.gcp.inbound.PubSubInboundChannelAdapter;
import org.springframework.integration.gcp.outbound.PubSubMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Created by joaomartins on 5/3/17.
 */
@SpringBootApplication
@IntegrationComponentScan
@RestController
@ComponentScan(basePackages = {"org.springframework.cloud.gcp"})
public class PubSubApplication {

  private static final Logger LOGGER = Logger.getGlobal();

  @Autowired
  private PubsubOutboundGateway messagingGateway;

  public static void main(String[] args) {
    SpringApplication.run(PubSubApplication.class, args);
  }

  @Bean
  public MessageChannel pubsubInputChannel() {
    return new PublishSubscribeChannel();
  }

  @Bean
  public MessageChannel orders() {
    return new PublishSubscribeChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter messageChannelAdapter(
      @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
      GcpProperties gcpProperties,
      GoogleCredentials credentials) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(gcpProperties.getProjectId(), "messages", credentials);
    adapter.setOutputChannel(inputChannel);

    return adapter;
  }

  @Bean
  public PubSubInboundChannelAdapter ordersChannelAdapter(
      @Qualifier("orders") MessageChannel inputChannel,
      GcpProperties gcpProperties,
      GoogleCredentials credentials) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(gcpProperties.getProjectId(), "orders", credentials);
    adapter.setOutputChannel(inputChannel);

    return adapter;
  }

  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler receiveMessage() {
    return message -> {
      LOGGER.info("Message arrived! Payload: "
          + ((ByteString) message.getPayload()).toStringUtf8());
      AckReplyConsumer consumer = (AckReplyConsumer) message.getHeaders().get(
          GcpHeaders.ACKNOWLEDGEMENT);
      consumer.ack();
    };
  }

  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler receiveMessageInParallel() {
    return message -> {
      LOGGER.info("Message also arrived here! "
          + ((ByteString) message.getPayload()).toStringUtf8());
      AckReplyConsumer consumer = (AckReplyConsumer) message.getHeaders().get(
          GcpHeaders.ACKNOWLEDGEMENT);
      consumer.ack();
    };
  }

  @Bean
  @ServiceActivator(inputChannel = "orders")
  public MessageHandler receiveOrder() {
    return message -> {
      LOGGER.info("Received an order!");
      AckReplyConsumer consumer = (AckReplyConsumer) message.getHeaders().get(
          GcpHeaders.ACKNOWLEDGEMENT);
      consumer.ack();
    };
  }

  @PostMapping("/postMessage")
  public RedirectView addMessage(@RequestParam("message") String message) {
    messagingGateway.sendToPubsub(message);
    return new RedirectView("/");
  }

  @Bean
  @ServiceActivator(inputChannel = "pubsubOutputChannel")
  public MessageHandler messageSender(GcpProperties gcpProperties,
      GoogleCredentials credentials) throws IOException {
    PubSubMessageHandler outboundAdapter =
        new PubSubMessageHandler(gcpProperties.getProjectId(), credentials);
    outboundAdapter.setTopic("test");
    return outboundAdapter;
  }

  @Bean
  public MessageChannel pubsubOutputChannel() {
    return new PublishSubscribeChannel();
  }

  @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
  public interface PubsubOutboundGateway {
    void sendToPubsub(String text);
  }

  @Bean
  public ExecutorProvider executorProvider() {
    return InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(8).build();
  }
}
