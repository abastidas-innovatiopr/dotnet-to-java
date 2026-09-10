package com.innovatiopr.payments.payments.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyCommand;
import com.innovatiopr.payments.payments.deposits.application.DepositMoneyHandler;
import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyCommand;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyHandler;
import com.innovatiopr.payments.payments.withdrawals.application.WithdrawMoneyCommand;
import com.innovatiopr.payments.payments.withdrawals.application.WithdrawMoneyHandler;
import com.innovatiopr.payments.shared.application.RequestHasher;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implements the Payments module's published contract by delegating to the slice handlers.
 *
 * <p>A thin adapter rather than a second implementation: the handlers keep the logic, this class only
 * translates the published signature into a command. Callers outside the module get a stable contract
 * while the slices inside stay free to change.
 */
@Service
class PaymentsApiAdapter implements PaymentsApi {

    private final TransferMoneyHandler transferMoney;
    private final DepositMoneyHandler depositMoney;
    private final WithdrawMoneyHandler withdrawMoney;

    PaymentsApiAdapter(TransferMoneyHandler transferMoney, DepositMoneyHandler depositMoney,
                       WithdrawMoneyHandler withdrawMoney) {
        this.transferMoney = transferMoney;
        this.depositMoney = depositMoney;
        this.withdrawMoney = withdrawMoney;
    }

    @Override
    public Result<UUID> transfer(String idempotencyKey, AccountId source, AccountId destination,
                                 BigDecimal amount, String currencyCode, String reference) {
        Result<IdempotencyKey> key = IdempotencyKey.create(idempotencyKey);
        if (key.isFailure()) {
            return key.propagate();
        }

        // Same canonical form the HTTP layer uses, so a caller here and a caller over HTTP produce the
        // same fingerprint for the same request.
        String canonical = String.join("|",
                String.valueOf(source), String.valueOf(destination),
                amount == null ? "" : amount.stripTrailingZeros().toPlainString(),
                currencyCode == null ? "" : currencyCode.toUpperCase(java.util.Locale.ROOT),
                reference == null ? "" : reference.trim());

        return transferMoney.handle(new TransferMoneyCommand(key.orElseThrow(),
                        RequestHasher.sha256(canonical), source, destination, amount, currencyCode, reference))
                .map(result -> result.transactionId());
    }

    @Override
    public Result<UUID> deposit(AccountId accountId, BigDecimal amount, String currencyCode, String reference) {
        return depositMoney.handle(new DepositMoneyCommand(accountId, amount, currencyCode, reference))
                .map(result -> result.transactionId());
    }

    @Override
    public Result<UUID> withdraw(AccountId accountId, BigDecimal amount, String currencyCode, String reference) {
        return withdrawMoney.handle(new WithdrawMoneyCommand(accountId, amount, currencyCode, reference))
                .map(result -> result.transactionId());
    }
}
