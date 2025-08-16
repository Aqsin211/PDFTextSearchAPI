package az.company.app.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static az.company.app.model.enums.ResponseMessages.UNEXPECTED_ERROR;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(DocumentNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse handle(DocumentNotFoundException exception) {
        log.error("Document not found: {}", exception.getMessage());
        return ErrorResponse.builder()
                .status(NOT_FOUND.value())
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handle(FileStorageException exception) {
        log.error("File storage error: {}", exception.getMessage());
        return ErrorResponse.builder()
                .status(INTERNAL_SERVER_ERROR.value())
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(DocumentProcessingException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handle(DocumentProcessingException exception) {
        log.error("Document processing error: {}", exception.getMessage());
        return ErrorResponse.builder()
                .status(INTERNAL_SERVER_ERROR.value())
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(SearchException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handle(SearchException exception) {
        log.error("Search error: {}", exception.getMessage());
        return ErrorResponse.builder()
                .status(INTERNAL_SERVER_ERROR.value())
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handle(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        return ErrorResponse.builder()
                .status(BAD_REQUEST.value())
                .messages(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception exception) {
        log.error("Unhandled exception: ", exception);
        return ErrorResponse.builder()
                .status(INTERNAL_SERVER_ERROR.value())
                .message(format(UNEXPECTED_ERROR.getMessage(), exception.getMessage()))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
