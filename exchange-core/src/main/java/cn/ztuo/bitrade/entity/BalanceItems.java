package cn.ztuo.bitrade.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceItems {
    String walletId;
    BigDecimal frozenAmount=BigDecimal.ZERO;
    BigDecimal availableAmount=BigDecimal.ZERO;
}
