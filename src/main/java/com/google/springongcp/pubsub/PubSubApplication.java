package com.google.springongcp.pubsub;

import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.springongcp.model.LoggableMessage;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.googlepubsub.channel.inbound.PubSubInboundChannelAdapter;
import org.springframework.integration.googlepubsub.channel.outbound.PubSubMessagingTemplate;
import org.springframework.integration.googlepubsub.channel.outbound.PubSubOutboundChannelAdapter;
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
@RestController
@ComponentScan(basePackages = {"org.springframework.integration.googlepubsub.channel"})
public class PubSubApplication {

  private static final Logger LOGGER = Logger.getGlobal();
  private MessageChannel outputChannel = new DirectChannel();

  @Autowired
  private Publisher publisher;
  @Autowired
  private PubSubMessagingTemplate pubsubTemplate;

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(PubSubApplication.class, args);
  }

  @Bean
  public MessageChannel pubsubInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter inboundChannelAdapter(
      @Qualifier("pubsubInputChannel") MessageChannel inputChannel) {
    PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter();
    adapter.setOutputChannel(inputChannel);

    return adapter;
  }

  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler receiveMessage() {
    return message -> {
      LOGGER.info("Message arrived!");
      if (message.getPayload() instanceof LoggableMessage) {
        LOGGER.info(((LoggableMessage) message.getPayload()).getBody());
      }
    };
  }

  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler receiveMessageInParallel() {
    return message -> LOGGER.info("Message also arrived here!");
  }

  @PostMapping("/postMessage")
  public RedirectView addMessage(@RequestParam("message") String message) {
    pubsubTemplate.convertAndSend(outputChannel, message);
    return new RedirectView("/");
  }

  @Bean
  @ServiceActivator(inputChannel = "pubsubOutputChannel")
  public MessageHandler messageSender() {
    return new PubSubOutboundChannelAdapter(pubSubMessagingTemplate());
  }

  @Bean
  public MessageChannel pubsubOutputChannel() {
    return outputChannel;
  }

  @Bean
  public PubSubMessagingTemplate pubSubMessagingTemplate() {
    return new PubSubMessagingTemplate(publisher);
  }
}
