package com.finalyearproject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public String handle404() {
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return "error/500";
    }
}
