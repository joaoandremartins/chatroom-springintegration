package com.google.springongcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

import java.util.logging.Logger;

/**
 * Created by joaomartins on 4/28/17.
 */
@Configuration
public class LoggingHandler {
  private Logger logger = Logger.getGlobal();

  @Bean
  @ServiceActivator(inputChannel = "amqpInputChannel")
  public MessageHandler handler() {
//    return new MessageHandler() {
//      @Override
//      public void handleMessage(org.springframework.messaging.LoggableMessage<?> message) throws MessagingException {
////        logger.info(((LoggableMessage)message.getPayload()).getBody());
//        handleMessage(message);
//      }
//    };

    return this::handleMessage;

//    {
//      if (message.getPayload() instanceof LoggableMessage) {
////        logger.info(((LoggableMessage)message.getPayload()).getBody());
//        handleMessage(message);
//      }
//    };
  }

  private <T> void handleMessage(org.springframework.messaging.Message<?> message) {
//    logger.info(((LoggableMessage)message.getPayload()).getBody());
  }
}
