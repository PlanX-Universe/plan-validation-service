package org.planx.validating.services

import org.planx.validating.exceptions.ValidationError
import org.planx.validating.functions.getLoggerFor
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.planx.common.models.systemmanager.validating.PlanValidationOut
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class VALService {
    private val logger = getLoggerFor<VALService>()

    private final val BASE_FOLDER = "/data/temp"
    private final val TOLERACE = 0.001

    fun validate(
        domain: String,
        problem: String,
        plan: String,
        requestId: String
    ): PlanValidationOut {
        val domainFileName = "$BASE_FOLDER/domain.pddl"
        val problemFileName = "$BASE_FOLDER/problem.pddl"
        val planFileName = "$BASE_FOLDER/plan.txt"

        // Write Files
        try {
            File(domainFileName).writeText(String(Base64.getDecoder().decode(domain)))
            File(problemFileName).writeText(String(Base64.getDecoder().decode(problem)))
            File(planFileName).writeText(plan)
        } catch (ex: Exception) {
            logger.error("Temp File creation failed: \n${ex.message}")
            throw ValidationError(
                requestId = requestId,
                internalErrors = ex.message
            )
        }

        val command = "./validate -v -t $TOLERACE $domainFileName $problemFileName $planFileName";
        logger.info("run command => \"$command\"")

        val valResultText: String? = command.runCommand(File("/root/projects/VAL/"), logger)
        logger.info("VAL RESULTS => \n\"$valResultText\"")

        return parseVALResults(valResultText, requestId)
    }

    private fun parseVALResults(out: String?, requestId: String): PlanValidationOut {
        if (out.isNullOrBlank()) {
            throw ValidationError(requestId = requestId, internalErrors = "Empty VAL output")
        }

        return PlanValidationOut(
            isValid = containsSuccessfulPlans(out) && !containsFailedPlans(out),
            fullOutput = out,
            plan = null
        )
    }

    private fun containsFailedPlans(out: String): Boolean {
        val failedPlansSectionRegex: Regex = "\n\nFailed plans:".toRegex()
        val failedToExecutePlanRegex: Regex = "\n\nPlan failed to execute\n".toRegex()
        return out.contains(failedPlansSectionRegex)
                || out.contains(failedToExecutePlanRegex)
    }

    private fun containsSuccessfulPlans(out: String): Boolean {
        val successfulPlansSectionRegex: Regex = "\nSuccessful plans:".toRegex()
        return out.contains(successfulPlansSectionRegex)
    }

    fun String.runCommand(workingDir: File, logger: Logger): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            return proc.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            logger.error(e.message)
            return null
        }
    }
}

