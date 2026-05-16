package com.finalyearproject.config;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public String handle404() {
        return "error/404";
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientDisconnect() {
        log.debug("Client disconnected (broken pipe) — suppressed");
    }

    @ExceptionHandler(Exception.class)
    public String handleAll(Exception e) {
        if (e instanceof IOException && "Broken pipe".equals(e.getMessage())) {
            log.debug("Broken pipe — suppressed");
            return null;
        }
        log.error("Unhandled exception", e);
        return "error/500";
    }
}
