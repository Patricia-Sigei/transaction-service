package com.wallet.transaction.repository;

import com.wallet.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findByFromWalletIdOrToWalletId(String fromWalletId, String toWalletId);
}