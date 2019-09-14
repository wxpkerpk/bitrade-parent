package cn.ztuo.bitrade.entity;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class RefundItem {
    Long userId;
    String coinSymbol;
    BigDecimal amount;
    ExchangeOrder order;
}
