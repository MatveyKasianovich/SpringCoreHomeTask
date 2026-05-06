package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class AccountProperties {

    private final BigDecimal default_amount;
    private final BigDecimal transfer_commission;

    public AccountProperties(@Value("${account.default-amount}") BigDecimal default_amount,
                             @Value("${account.transfer-commission}") BigDecimal transfer_commission) {
        this.default_amount = default_amount;
        this.transfer_commission = transfer_commission;
    }

    public BigDecimal getDefault_amount() {
        return default_amount;
    }

    public BigDecimal getTransfer_commission() {
        return transfer_commission;
    }
}