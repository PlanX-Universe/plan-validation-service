package org.planx.validating

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ValidatingServiceApplication

fun main(args: Array<String>) {
    runApplication<ValidatingServiceApplication>(*args)
}
