package com.bitclave.node.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.CONFLICT)
class DuplicateException : RuntimeException("Item already exist in system") {}
