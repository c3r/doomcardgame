package pl.c3r.doomcardgame.controllers;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResponseDTO {
    String errorCode;
    String errorMessage;
    String returnCode;
    String message;
    Object responseEntity;
}