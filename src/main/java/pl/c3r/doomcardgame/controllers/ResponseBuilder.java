package pl.c3r.doomcardgame.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseBuilder {

    public ResponseEntity<ResponseDTO> buildResponse(Object entity, HttpStatus status) {
        ResponseDTO dto = ResponseDTO
                .builder()
                .responseEntity(entity)
                .build();

        return buildResponse(dto, status);
    }

    private ResponseEntity<ResponseDTO> buildResponse(ResponseDTO dto, HttpStatus status) {
        return ResponseEntity.status(status).body(dto);
    }

    public ResponseEntity<ResponseDTO> buildErrorResponse(String errorMessage, HttpStatus status) {
        ResponseDTO dto = ResponseDTO.builder()
                .errorMessage(errorMessage)
                .errorCode(status.toString())
                .build();

        return buildResponse(dto, status);
    }

}
