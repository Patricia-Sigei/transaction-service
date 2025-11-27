package com.wallet.transaction.client;

import com.wallet.transaction.dto.BalanceUpdateRequest;
import com.wallet.transaction.dto.WalletResponse;
import com.wallet.transaction.exception.WalletServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Component
@RequiredArgsConstructor
public class WalletClient {

    private final RestTemplate restTemplate;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    public WalletResponse getWallet(String walletId) {
        try {
            String url = walletServiceUrl + "/api/wallets/" + walletId;
            ResponseEntity<WalletResponse> response = restTemplate.getForEntity(url, WalletResponse.class);
            return response.getBody();
        } catch (Exception e) {
            throw new WalletServiceException("Failed to fetch wallet: " + e.getMessage());
        }
    }

    public WalletResponse updateBalance(String walletId, BalanceUpdateRequest request) {
        try {
            String url = walletServiceUrl + "/api/wallets/" + walletId + "/balance";
            restTemplate.put(url, request);
            return getWallet(walletId);
        } catch (Exception e) {
            throw new WalletServiceException("Failed to update wallet balance: " + e.getMessage());
        }
    }
}