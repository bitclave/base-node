package com.bitclave.node.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class BadArgumentException(message: String = "bad arguments") : RuntimeException(message)
