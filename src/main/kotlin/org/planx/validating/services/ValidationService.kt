package org.planx.validating.services

import org.planx.validating.functions.getLoggerFor
import org.planx.validating.messaging.producer.Sender
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import org.planx.common.models.CallStack
import org.planx.common.models.MutableStack
import org.planx.common.models.endpoint.solving.plan.Action
import org.planx.common.models.endpoint.solving.plan.Plan
import org.planx.common.models.endpoint.solving.plan.PlanXAction
import org.planx.common.models.endpoint.solving.plan.sequential.SequentialPlan
import org.planx.common.models.systemmanager.validating.PlanValidationOut
import org.planx.common.models.systemmanager.validating.PlanValidationRequest

@Service
class ValidationService(
    private val valService: VALService,
    private val sender: Sender
) {
    private val logger = getLoggerFor<ValidationService>()

    fun validatePlan(
        content: PlanValidationRequest<*>,
        requestId: String,
        callStack: MutableStack<CallStack>
    ): Mono<PlanValidationOut> {
        logger.info("Start Planning for $requestId")

        if (!callStack.isEmpty()) {
            // remove current step
            callStack.pop()
        }

        // convert to IPC universal output format
        val ipcUniversalString: String = planToIpcUniversalString(content.plan!! as SequentialPlan<PlanXAction>)
        logger.info("Plan conversion results: \n$ipcUniversalString")

        return Mono.just(
            valService.validate(
                domain = content.domain!!,
                problem = content.problem!!,
                plan = ipcUniversalString,
                requestId = requestId
            ).apply {
                this.plan = (content.plan!! as SequentialPlan<PlanXAction>)
            }
        )
    }

    fun sendValidationResults(
        result: PlanValidationOut,
        requestId: String,
        callStack: MutableStack<CallStack>
    ) {
        logger.info("Sending Validation results: $result")
        sender.sendValidationResults(
            result = result,
            requestId = requestId,
            callStack = callStack
        )
    }

    /**
     * FIXME: This method is currently not meeting the standard!
     */
    private fun planToIpcUniversalString(plan: SequentialPlan<PlanXAction>): String {
        return plan.actions!!
            .map {
                val sb = StringBuilder()
                sb.append("${it.momentInTime}: (${it.name} ${it.instantiations!!.joinToString(" ")})")
                if (it.preconditions is List<*> && (it.preconditions as List<*>).isNotEmpty()) {
                    sb.append(
                        " [${
                            (it.preconditions as List<*>).joinToString(" ")
                        }]"
                    )
                }
                sb.toString()
            }
            .joinToString("\n")
    }
}
