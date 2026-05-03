package com.resumade.export.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${export.queue.name}")
    private String queueName;

    @Value("${export.queue.exchange}")
    private String exchange;

    @Value("${export.queue.routing-key}")
    private String routingKey;

    @Bean
    public Queue exportQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange exportExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Binding binding(Queue exportQueue, DirectExchange exportExchange) {
        return BindingBuilder.bind(exportQueue).to(exportExchange).with(routingKey);
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
    }

    @Bean
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.core.RabbitTemplate template = new org.springframework.amqp.rabbit.core.RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
