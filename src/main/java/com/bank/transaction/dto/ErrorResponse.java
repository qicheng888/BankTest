package com.bank.transaction.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error Response DTO (Record)
 * 
 * JDK 21 Record class for standardized error response format.
 */
public record ErrorResponse(int status,String error,String message,String path,LocalDateTime timestamp,Map<String,String>validationErrors){
/**
 * Create an ErrorResponse without validation errors
 */
public static ErrorResponse of(int status,String error,String message,String path){return new ErrorResponse(status,error,message,path,LocalDateTime.now(),null);}

/**
 * Create an ErrorResponse with validation errors
 */
public static ErrorResponse withValidationErrors(int status,String error,String message,String path,Map<String,String>validationErrors){return new ErrorResponse(status,error,message,path,LocalDateTime.now(),validationErrors);}}
