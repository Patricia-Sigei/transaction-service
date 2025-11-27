package com.wallet.transaction.service;

import com.wallet.transaction.client.WalletClient;
import com.wallet.transaction.dto.*;
import com.wallet.transaction.entity.Transaction;
import com.wallet.transaction.entity.TransactionStatus;
import com.wallet.transaction.entity.TransactionType;
import com.wallet.transaction.exception.TransactionFailedException;
import com.wallet.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletClient walletClient;

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString();

        try {
            // Verify wallet exists
            walletClient.getWallet(request.getWalletId());

            // Update wallet balance
            BalanceUpdateRequest balanceUpdate = new BalanceUpdateRequest(request.getAmount());
            walletClient.updateBalance(request.getWalletId(), balanceUpdate);

            // Record transaction
            Transaction transaction = new Transaction();
            transaction.setTransactionId(transactionId);
            transaction.setFromWalletId("SYSTEM");
            transaction.setToWalletId(request.getWalletId());
            transaction.setAmount(request.getAmount());
            transaction.setType(TransactionType.DEPOSIT);
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setDescription(request.getDescription());

            Transaction saved = transactionRepository.save(transaction);
            return mapToResponse(saved);

        } catch (Exception e) {
            // Record failed transaction
            Transaction failedTransaction = new Transaction();
            failedTransaction.setTransactionId(transactionId);
            failedTransaction.setFromWalletId("SYSTEM");
            failedTransaction.setToWalletId(request.getWalletId());
            failedTransaction.setAmount(request.getAmount());
            failedTransaction.setType(TransactionType.DEPOSIT);
            failedTransaction.setStatus(TransactionStatus.FAILED);
            failedTransaction.setDescription("Failed: " + e.getMessage());

            transactionRepository.save(failedTransaction);
            throw new TransactionFailedException("Deposit failed: " + e.getMessage());
        }
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString();

        try {
            // Verify wallet exists and has sufficient balance
            WalletResponse wallet = walletClient.getWallet(request.getWalletId());
            if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new TransactionFailedException("Insufficient balance");
            }

            // Update wallet balance (negative amount for withdrawal)
            BalanceUpdateRequest balanceUpdate = new BalanceUpdateRequest(request.getAmount().negate());
            walletClient.updateBalance(request.getWalletId(), balanceUpdate);

            // Record transaction
            Transaction transaction = new Transaction();
            transaction.setTransactionId(transactionId);
            transaction.setFromWalletId(request.getWalletId());
            transaction.setToWalletId("SYSTEM");
            transaction.setAmount(request.getAmount());
            transaction.setType(TransactionType.WITHDRAWAL);
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setDescription(request.getDescription());

            Transaction saved = transactionRepository.save(transaction);
            return mapToResponse(saved);

        } catch (Exception e) {
            // Record failed transaction
            Transaction failedTransaction = new Transaction();
            failedTransaction.setTransactionId(transactionId);
            failedTransaction.setFromWalletId(request.getWalletId());
            failedTransaction.setToWalletId("SYSTEM");
            failedTransaction.setAmount(request.getAmount());
            failedTransaction.setType(TransactionType.WITHDRAWAL);
            failedTransaction.setStatus(TransactionStatus.FAILED);
            failedTransaction.setDescription("Failed: " + e.getMessage());

            transactionRepository.save(failedTransaction);
            throw new TransactionFailedException("Withdrawal failed: " + e.getMessage());
        }
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString();

        try {
            // Verify both wallets exist
            WalletResponse fromWallet = walletClient.getWallet(request.getFromWalletId());
            walletClient.getWallet(request.getToWalletId());

            // Check sufficient balance
            if (fromWallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new TransactionFailedException("Insufficient balance in source wallet");
            }

            // Deduct from source wallet
            BalanceUpdateRequest deductBalance = new BalanceUpdateRequest(request.getAmount().negate());
            walletClient.updateBalance(request.getFromWalletId(), deductBalance);

            // Add to destination wallet
            BalanceUpdateRequest addBalance = new BalanceUpdateRequest(request.getAmount());
            walletClient.updateBalance(request.getToWalletId(), addBalance);

            // Record transaction
            Transaction transaction = new Transaction();
            transaction.setTransactionId(transactionId);
            transaction.setFromWalletId(request.getFromWalletId());
            transaction.setToWalletId(request.getToWalletId());
            transaction.setAmount(request.getAmount());
            transaction.setType(TransactionType.TRANSFER);
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setDescription(request.getDescription());

            Transaction saved = transactionRepository.save(transaction);
            return mapToResponse(saved);

        } catch (Exception e) {
            // Record failed transaction
            Transaction failedTransaction = new Transaction();
            failedTransaction.setTransactionId(transactionId);
            failedTransaction.setFromWalletId(request.getFromWalletId());
            failedTransaction.setToWalletId(request.getToWalletId());
            failedTransaction.setAmount(request.getAmount());
            failedTransaction.setType(TransactionType.TRANSFER);
            failedTransaction.setStatus(TransactionStatus.FAILED);
            failedTransaction.setDescription("Failed: " + e.getMessage());

            transactionRepository.save(failedTransaction);
            throw new TransactionFailedException("Transfer failed: " + e.getMessage());
        }
    }

    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionFailedException("Transaction not found: " + transactionId));
        return mapToResponse(transaction);
    }

    public List<TransactionResponse> getWalletTransactions(String walletId) {
        List<Transaction> transactions = transactionRepository.findByFromWalletIdOrToWalletId(walletId, walletId);
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionId(),
                transaction.getFromWalletId(),
                transaction.getToWalletId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getTimestamp(),
                transaction.getDescription()
        );
    }
}