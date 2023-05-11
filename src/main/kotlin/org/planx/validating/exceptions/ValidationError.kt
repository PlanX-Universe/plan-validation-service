package org.planx.validating.exceptions

import org.planx.common.models.BaseException

class ValidationError(
    override val requestId: String = "",
    override var internalErrors: String? = ""
) : BaseException()
