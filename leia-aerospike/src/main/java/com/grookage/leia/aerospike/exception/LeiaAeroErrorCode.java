package com.grookage.leia.aerospike.exception;

import com.grookage.leia.models.exception.LeiaErrorCode;
import lombok.Getter;

@Getter
public enum LeiaAeroErrorCode implements LeiaErrorCode {

    INDEX_CREATION_FAILED(412),

    WRITE_FAILED(500);

    final int status;

    LeiaAeroErrorCode(int status) {
        this.status = status;
    }
}
