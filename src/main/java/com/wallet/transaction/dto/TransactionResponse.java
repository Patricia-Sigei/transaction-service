package com.wallet.transaction.dto;

import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private String fromWalletId;
    private String toWalletId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private String description;
}