package com.google.springongcp.pubsub;

import com.google.springongcp.model.LoggableMessage;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.googlepubsub.channel.Subscriber;
import org.springframework.messaging.Message;

/**
 * Created by joaomartins on 5/3/17.
 */
@SpringBootApplication
public class PubSubApplication {

  private static final Logger LOGGER = Logger.getGlobal();

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(PubSubApplication.class, args);
  }

  @Publisher(channel = "topic")
  public Object sendMessage() {
    return new LoggableMessage("joao", "Hello world!", LocalDateTime.now());
  }

  @Subscriber(topic = "topic")
  public void receiveMessage(Message<?> message) {
    LOGGER.info("Message arrived!");
    if (message.getPayload() instanceof LoggableMessage) {
      LOGGER.info(((LoggableMessage) message.getPayload()).getBody());
    }
  }

  @Subscriber(topic = "topic")
  public void receiveMessageInParallel(Message<?> message) {
    LOGGER.info("Message also arrived here!");
  }
}
