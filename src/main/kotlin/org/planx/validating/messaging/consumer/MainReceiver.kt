package org.planx.validating.messaging.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.planx.validating.exceptions.ValidationError
import org.planx.validating.functions.getLoggerFor
import org.planx.validating.services.ValidationService
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.planx.common.models.CallStack
import org.planx.common.models.MutableStack
import org.planx.common.models.endpoint.solving.plan.Action
import org.planx.common.models.endpoint.solving.plan.Plan
import org.planx.common.models.endpoint.solving.plan.PlanXAction
import org.planx.common.models.endpoint.solving.plan.sequential.SequentialPlan
import org.planx.common.models.systemmanager.validating.PlanValidationRequest
import org.planx.common.models.systemmanager.validating.PlanValidationRequestBody
import org.planx.common.models.systemmanager.validating.pddl.PddlPlanValidationRequest
import org.planx.common.models.systemmanager.validating.pddl.PddlPlanValidationRequestBody

@Component
@RabbitListener(queues = ["\${planx.queues.request.name}"])
class MainReceiver(
    private var validationService: ValidationService,
    private val objectMapper: ObjectMapper
) {
    var logger = getLoggerFor<MainReceiver>()

    @RabbitHandler
    fun handleMessage(row: Any) {
        // FIXME: casting it directly leads to an error
        val rawMessage: Message = row as Message
        val body = String(rawMessage.body)
        val message: PddlPlanValidationRequestBody =
            objectMapper.readValue(body, PddlPlanValidationRequestBody::class.java)
        logger.info("Request received! \n (RequestID = ${message.requestId})")
        validationService.validatePlan(
            content = message.content!!,
            requestId = message.requestId,
            callStack = message.callStack
        ).doOnError {
            throw ValidationError(message.requestId, it.message)
        }.subscribe { isValid ->
            validationService.sendValidationResults(
                requestId = message.requestId,
                callStack = message.callStack,
                result = isValid
            )
        }
//        validationService.validatePlan(
//            content = PddlPlanValidationRequest(
//                domain = "KGRlZmluZSAoZG9tYWluIGRyaXZlZG9tYWluKQooOnJlcXVpcmVtZW50cyA6Zmx1ZW50cyA6Y29udGludW91cy1lZmZlY3RzIDpuZWdhdGl2ZS1wcmVjb25kaXRpb25zIDpkdXJhdGlvbi1pbmVxdWFsaXRpZXMgOnRpbWUgOnR5cGluZykKKDp0eXBlcyB2ZWhpY2xlIGxvY2F0aW9uKQooOnByZWRpY2F0ZXMgKGF0ID92IC0gdmVoaWNsZSA/bCAtIGxvY2F0aW9uKSkKCig6ZnVuY3Rpb25zIChtYXhzcGVlZCA/diAtIHZlaGljbGUpIChzcGVlZCA/diAtIHZlaGljbGUpICh0cmF2ZWx0aW1lID92IC0gdmVoaWNsZSkgKGRpc3RhbmNlID9wID9sIC0gbG9jYXRpb24pKQoKCjs7IFNldCB0aGUgYWNjZWxlcmF0b3IgdmFsdWUgb3ZlciB0aW1lLiBVc2VzIHRpbWUgYXMgYSAKOzsgd2F5IG9mIGF2b2lkaW5nIG1vZGVsbGluZyBhY2NlbGVyYXRpb24gYXMgYSBudW1lcmljCjs7IHBhcmFtZXRlci4KCig6ZHVyYXRpdmUtYWN0aW9uIGFjY2VsZXJhdGUKOnBhcmFtZXRlcnMgKD92IC0gdmVoaWNsZSkKOmR1cmF0aW9uIChhbmQgKDw9ID9kdXJhdGlvbiAoLSAobWF4c3BlZWQgP3YpIChzcGVlZCA/dikpKSkKOmNvbmRpdGlvbiAoYW5kICkKOmVmZmVjdCAoYW5kIChhdCBlbmQgKGluY3JlYXNlIChzcGVlZCA/dikgP2R1cmF0aW9uKSkpCikKCjs7IFRoZW4gZHJpdmUgd2l0aCB0aGUgYWNjZWxlcmF0b3Igc2V0dGluZy4KCig6YWN0aW9uIGRyaXZlCjpwYXJhbWV0ZXJzICg/diAtIHZlaGljbGUgP3AgP2wgLSBsb2NhdGlvbikKOnByZWNvbmRpdGlvbiAoYW5kIChhdCA/diA/cCkgKD4gKHNwZWVkID92KSAwKSkKOmVmZmVjdCAoYW5kIChhdCA/diA/bCkgKG5vdCAoYXQgP3YgP3ApKQooaW5jcmVhc2UgKHRyYXZlbHRpbWUgP3YpICgvIChkaXN0YW5jZSA/cCA/bCkgKHNwZWVkID92KSkpKQopCgoKKQo=",
//                problem = "KGRlZmluZSAocHJvYmxlbSBkcml2ZS1wcm9ibGVtKQooOmRvbWFpbiBkcml2ZWRvbWFpbikKKDpvYmplY3RzICAKCWNhciAtIHZlaGljbGUKCXN0YXJ0IC0gbG9jYXRpb24KCWVuZCAtIGxvY2F0aW9uCikKKDppbml0IAoKCShhdCBjYXIgc3RhcnQpCgkoPSAoc3BlZWQgY2FyKSAwKQoJKD0gKG1heHNwZWVkIGNhcikgMTApCgkoPSAodHJhdmVsdGltZSBjYXIpIDApCgkoPSAoZGlzdGFuY2Ugc3RhcnQgZW5kKSAxMDApCikKKDpnb2FsIChhbmQgKGF0IGNhciBlbmQpICg+PSAodHJhdmVsdGltZSBjYXIpIDIwKSkpCig6bWV0cmljIG1pbmltaXplICh0b3RhbC10aW1lKSkpIAo=",
//                plan = SequentialPlan(
//                    cost = 2.0,
//                    actions = listOf(
//                        PlanXAction(
//                            name = "accelerate",
//                            momentInTime = 0,
//                            instantiations = listOf("car"),
//                            preconditions = listOf(5),
//                            cost = 6.0
//                        ),
//                        PlanXAction(
//                            name = "drive",
//                            momentInTime = 6,
//                            instantiations = listOf("car", "start", "end"),
//                            cost = 1.0
//                        )
//                    )
//                )
//            ),
//            requestId = message.requestId,
//            callStack = message.callStack
//        ).doOnError {
//            throw ValidationError("DUMMY-ID-REPLACE!", it.message)
//        }.subscribe { planValidationObj ->
//            validationService.sendValidationResults(
//                requestId = message.requestId,
//                callStack = message.callStack,
//                // TODO: add full VAL output
//                result = planValidationObj
//            )
//        }
    }
}
