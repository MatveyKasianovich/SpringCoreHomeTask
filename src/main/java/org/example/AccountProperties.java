package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountProperties {

    private final float default_amount;
    private final float transfer_commission;

    public AccountProperties(@Value("${account.default-amount}") float default_amount,
                             @Value("${account.transfer-commission}") float transfer_commission
    ){this.default_amount=default_amount;
      this.transfer_commission=transfer_commission;
    }

    public float getDefault_amount() {
        return default_amount;
    }

    public float getTransfer_commission() {
        return transfer_commission;
    }
}
