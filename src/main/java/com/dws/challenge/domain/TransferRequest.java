package com.dws.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {

    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
}
