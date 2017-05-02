package com.google.springongcp;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@SpringBootApplication
@IntegrationComponentScan
@RestController
public class ChatroomSpringintegrationApplication {

  private static final String QUEUE_NAME = "chan";
  private static OutboundGateway gateway;

  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        SpringApplication.run(ChatroomSpringintegrationApplication.class, args);
    gateway = context.getBean(OutboundGateway.class);
  }

  @Bean
  public Queue chanQueue() {
    return new Queue(QUEUE_NAME);
  }

  @Bean
  public MessageChannel amqpInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public AmqpInboundChannelAdapter inbound(SimpleMessageListenerContainer listenerContainer,
                                           @Qualifier("amqpInputChannel") MessageChannel channel)
  {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory) {
    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
    container.setQueueNames(QUEUE_NAME);
    container.setConcurrentConsumers(2);

    return container;
  }

  @PostMapping("/postMessage")
  public void postMessage(@RequestParam("message") String message) {
    gateway.sendToRabbit(new LoggableMessage("joao", message, LocalDateTime.now()));
  }

  @Bean
  @ServiceActivator(inputChannel = "amqpOutboundChannel")
  public AmqpOutboundEndpoint amqpOutbound(AmqpTemplate amqpTemplate) {
    AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
    outbound.setRoutingKey(QUEUE_NAME);
    return outbound;
  }

  @Bean
  public MessageChannel amqpOutboundChannel() {
    return new DirectChannel();
  }

  @MessagingGateway(defaultRequestChannel = "amqpOutboundChannel")
  public interface OutboundGateway {
    void sendToRabbit(LoggableMessage message);
  }
}
