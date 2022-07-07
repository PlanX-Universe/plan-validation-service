package org.planx.validating.messaging.producer

import org.planx.validating.functions.getLoggerFor
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import org.planx.common.models.BaseMessageInterface
import org.planx.common.models.CallStack
import org.planx.common.models.MutableStack
import org.planx.common.models.endpoint.solving.plan.PlanXAction
import org.planx.common.models.endpoint.solving.plan.sequential.SequentialPlan
import org.planx.common.models.systemmanager.validating.PlanValidationResponse
import org.planx.common.models.systemmanager.validating.PlanValidationResponseBody
import org.planx.common.models.systemmanager.validating.pddl.PddlPlanValidationResponseBody

@Component
class Sender(var rabbitTemplate: RabbitTemplate) {
    var logger = getLoggerFor<Sender>()

    fun sendValidationResults(
        result: PlanValidationResponse<SequentialPlan<PlanXAction>>,
        requestId: String,
        callStack: MutableStack<CallStack>
    ) {
        val responseBody = PddlPlanValidationResponseBody(
            requestId = requestId,
            callStack = callStack,
            content = result
        )
        send<PlanValidationResponseBody>(content = responseBody, addressElement = callStack.peek())
    }

    /**
     * generic send function
     */
    private fun <M> send(content: M, addressElement: CallStack) where M : BaseMessageInterface {
        try {
            logger.info("response send to ${addressElement.topic} (RequestId: ${content.requestId})")
            // is adding "__TypeId__" automatically
            rabbitTemplate.convertAndSend(addressElement.topic, addressElement.routingKey, content)
        } catch (e: AmqpException) {
            if (e.message != null) logger.error(e.message) else logger.error("Unknown AmqpException was thrown!")
        }
    }
}
