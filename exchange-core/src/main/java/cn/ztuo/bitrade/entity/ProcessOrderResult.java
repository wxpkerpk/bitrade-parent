package cn.ztuo.bitrade.entity;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessOrderResult {
    String pair;
    ExchangeOrderDetail exchangeOrderDetail;//成交明细
    OrderDetailAggregation orderDetailAggregation;//币币交易订单手续费明细
    MemberTransaction incomeMemberTransaction;//会员交易记录，包括充值、提现、转账等
    MemberTransaction outcomeMemberTransaction;//会员交易记录，包括充值、提现、转账等

    Long userId;
    String incomeSymbol;
    String outcomeSymbol;
    BigDecimal incomeCoinAmount;
    BigDecimal outcomeCoinAmount;
    BigDecimal fee;
}
