package org.linghu.mybackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 业务逻辑异常
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends BaseException {
    
    private static final int DEFAULT_CODE = 400;
    
    public BusinessException(String message) {
        super(DEFAULT_CODE, message);
    }
    
    public BusinessException(int code, String message) {
        super(code, message);
    }
}
