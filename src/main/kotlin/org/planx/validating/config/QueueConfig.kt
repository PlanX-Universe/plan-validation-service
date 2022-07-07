package org.planx.validating.config

import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * This config sets up all queues for the given service topic
 *
 * @author Sebastian Graef
 */
@Configuration
class QueueConfigs : MainQueueConfigInterface {

    @Value("\${planx.queues.dlq.key}")
    private lateinit var deadLetterQueueRoutingKey: String

    @Bean
    override fun mainQueue(
        @Value(value = "\${planx.queues.request.name}") mainQueueName: String
    ): Queue {
        return QueueBuilder.nonDurable(mainQueueName)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", deadLetterQueueRoutingKey)
            .build()
    }

    @Bean
    override fun deadLetterQueue(
        @Value(value = "\${planx.queues.dlq.name}") deadLetterQueueName: String
    ): Queue {
        return QueueBuilder.durable(deadLetterQueueName).build()
    }
}

interface MainQueueConfigInterface {

    /**
     * Method to get the main topic in queue.
     *
     * It requires the environment property "planx.queues.request.name"!
     */
    fun mainQueue(@Value("\${planx.queues.request.name}") mainQueueName: String): Queue

    /**
     * Method to get the dead letter queue.
     *
     * It requires the environment property "planx.queues.dlq.name"!
     */
    fun deadLetterQueue(@Value("\${planx.queues.dlq.name}") deadLetterQueueName: String): Queue
}