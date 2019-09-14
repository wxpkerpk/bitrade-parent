package cn.ztuo.bitrade.entity;


import lombok.Data;

@Data
public class ProcessTradeMessage {
    ProcessOrderResult buyOrderProcessResult;
    ProcessOrderResult sellOrderProcessResult;
    RefundItem buyRefund;
    RefundItem sellRefund;

}
