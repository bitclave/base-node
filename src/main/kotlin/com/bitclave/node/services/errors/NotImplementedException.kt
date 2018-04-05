package com.bitclave.node.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
class NotImplementedException : RuntimeException("Not implemented") {}
