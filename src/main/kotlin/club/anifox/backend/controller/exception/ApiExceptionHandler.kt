package club.anifox.backend.controller.exception

import club.anifox.backend.domain.exception.common.BadRequestException
import club.anifox.backend.domain.exception.common.ConflictException
import club.anifox.backend.domain.exception.common.NoContentException
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.domain.exception.common.UnauthorizedException
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.ConnectException

@ControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(NoContentException::class)
    fun handleNoContentException(
        response: HttpServletResponse,
        ex: NoContentException,
        request: WebRequest,
    ) {
        response.status = HttpStatus.NO_CONTENT.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"message\": \"${ex.message}\"}")
        response.writer.flush()
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        response: HttpServletResponse,
        ex: BadRequestException,
        request: WebRequest,
    ) {
        response.status = HttpStatus.BAD_REQUEST.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"error\": \"${ex.message}\"}")
        response.writer.flush()
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedRequest(
        response: HttpServletResponse,
        request: WebRequest,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.flush()
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundRequest(
        response: HttpServletResponse,
        ex: NotFoundException,
        request: WebRequest,
    ) {
        response.status = HttpStatus.NOT_FOUND.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"error\": \"${ex.message}\"}")
        response.writer.flush()
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(
        response: HttpServletResponse,
        ex: BadCredentialsException,
        request: WebRequest,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"error\": \"${ex.message}\"}")
        response.writer.flush()
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        response: HttpServletResponse,
        ex: ConflictException,
        request: WebRequest,
    ) {
        response.status = HttpStatus.CONFLICT.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"error\": \"${ex.message}\"}")
        response.writer.flush()
    }

    @ExceptionHandler(ConnectException::class)
    fun handleConnect(
        response: HttpServletResponse,
        ex: ConflictException,
        request: WebRequest,
    ) {
        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("{\"error\": \"${ex.message}\"}")
        response.writer.flush()
    }
}
