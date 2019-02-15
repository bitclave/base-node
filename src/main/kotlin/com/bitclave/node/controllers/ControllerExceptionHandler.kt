package com.bitclave.node.controllers

import com.bitclave.node.services.errors.*
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.util.WebUtils

private val logger = KotlinLogging.logger {}

@ControllerAdvice
class ControllerExceptionHandler {

    /** Provides handling for exceptions throughout this service.  */
    @ExceptionHandler(AccessDeniedException::class, AlreadyRegisteredException::class, BadArgumentException::class, DataNotSavedException::class,
            DuplicateException::class, NotFoundException::class, NotImplementedException::class)
    fun handleException(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        val headers = HttpHeaders()

        if (ex is AccessDeniedException) {
            val status = HttpStatus.FORBIDDEN
            val errors = ex.message
            return handleExceptionInternal(ex, errors, headers, status, request)
        } else if (ex is AlreadyRegisteredException) {
            val status = HttpStatus.CONFLICT
            val errors = ex.message
            return handleExceptionInternal(ex, errors, headers, status, request)
        } else if (ex is BadArgumentException) {
            val status = HttpStatus.BAD_REQUEST
            val errors = ex.message
            return handleExceptionInternal(ex, errors, headers, status, request)
        } else if (ex is DataNotSavedException) {
            val status = HttpStatus.INTERNAL_SERVER_ERROR
            val errors = ex.message
            return handleExceptionInternal(ex, errors, headers, status, request)
        } else if (ex is DuplicateException) {
            val status = HttpStatus.CONFLICT
            val errors = ex.message
            return handleExceptionInternal(ex, errors, headers, status, request)
        } else if (ex is NotFoundException) {
            val status = HttpStatus.NOT_FOUND
            val errors = ex.message
            return handleExceptionInternal(ex, errors, headers, status, request)
        } else if (ex is NotImplementedException) {
            val status = HttpStatus.NOT_IMPLEMENTED
            val errors = ex.message
            return handleExceptionInternal(ex, errors, headers, status, request)
        } else {
            val status = HttpStatus.INTERNAL_SERVER_ERROR
            return handleExceptionInternal(ex, null, headers, status, request)
        }
    }

    /** A single place to customize the response body of all Exception types.  */
    protected fun handleExceptionInternal(ex: Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        if (HttpStatus.INTERNAL_SERVER_ERROR == status) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST)
        }
        logger.error("Request: " + request.toString() + " raised " + ex)
        return ResponseEntity<Any>(body, headers, status)
    }
}
