package zerobase.dividend.exception.impl;

import org.springframework.http.HttpStatus;
import zerobase.dividend.exception.AbstractException;

public class AlreadyExistCompanyException extends AbstractException {
    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "이미 존재하는 회사입니다.";
    }
}
