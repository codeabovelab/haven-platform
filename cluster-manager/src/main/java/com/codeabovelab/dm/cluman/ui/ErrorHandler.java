/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.ui;


import com.codeabovelab.dm.cluman.model.NotFoundException;
import com.codeabovelab.dm.cluman.ui.model.UiError;
import com.codeabovelab.dm.common.security.token.TokenException;
import com.codeabovelab.dm.common.utils.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.*;

/**
 * Error handler: format error messages
 */
@ControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(BindException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public UiError bindErrorHandler(BindException bindException) {
        log.error("Can't process request", bindException);
        return createResponse(bindException.getMessage(), bindException.getAllErrors().toString(), BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public UiError bindErrorHandler(MethodArgumentNotValidException bindException) {
        log.error("Can't process request", bindException);
        return createResponse(bindException.getMessage(), bindException.getBindingResult().getAllErrors().toString(), BAD_REQUEST);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public UiError bindErrorHandler(IllegalArgumentException e) {
        log.error("Can't process request", e);
        return createResponse(e.getMessage(), Throwables.printToString(e), BAD_REQUEST);
    }

    @ExceptionHandler({NotFoundException.class})
    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    public UiError bindErrorHandler(NotFoundException e) {
        log.error("Can't process request", e);
        return createResponse(e.getMessage(), Throwables.printToString(e), NOT_FOUND);
    }

    @ExceptionHandler({HttpClientErrorException.class})
    @ResponseBody
    public ResponseEntity<UiError> bindErrorHandler(HttpClientErrorException e) {
        log.error("Can't process request", e);
        HttpStatus statusCode = e.getStatusCode();
        UiError response = createResponse(StringUtils.trimWhitespace(e.getResponseBodyAsString()),
                Throwables.printToString(e), statusCode);
        return new ResponseEntity<>(response, BAD_REQUEST);
    }

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<UiError> handleHttpExcepion(HttpException ex) {
        log.error("Can't process request", ex);
        UiError error = UiError.from(ex);
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler()
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    public UiError internalError(HttpServletRequest req, Exception e) {
        log.error("Can't process request", e);
        return createResponse(e.getMessage(), Throwables.printToString(e), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({TokenException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public UiError badCredentialsException(Exception e) {
        log.error("Can't process request", e);
        return createResponse(e.getMessage(), e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    static UiError createResponse(String message, String stackTrace, HttpStatus httpStatus) {
        UiError uiError = new UiError();
        uiError.setStack(stackTrace);
        uiError.setMessage(message);
        uiError.setCode(httpStatus.value());
        return uiError;
    }

}