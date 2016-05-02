package com.ribay.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by CD on 02.05.2016.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Bad request. Check documentation of ribay's rest api!")
public class BadRequestException extends Exception {
}
