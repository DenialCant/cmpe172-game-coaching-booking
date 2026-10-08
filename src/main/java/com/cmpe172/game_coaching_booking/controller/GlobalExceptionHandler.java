package com.cmpe172.game_coaching_booking.controller;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import com.cmpe172.game_coaching_booking.exception.ForbiddenException;
import com.cmpe172.game_coaching_booking.exception.InvalidRequestException;
import com.cmpe172.game_coaching_booking.exception.NotFoundException;
import com.cmpe172.game_coaching_booking.exception.SlotConflictException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ModelAndView badRequest(InvalidRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConversionFailedException.class})
    public ModelAndView invalidParameter(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "A request value has an invalid format");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView forbidden(ForbiddenException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(SlotConflictException.class)
    public ModelAndView conflict(SlotConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ModelAndView error(HttpStatus status, String message) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("title", status.getReasonPhrase());
        modelAndView.addObject("message", message);
        return modelAndView;
    }
}
