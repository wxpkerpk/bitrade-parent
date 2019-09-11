package cn.ztuo.bitrade.component;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author zkq
 * @create 2019-02-22 21:17
 **/
@Data
public class KLineInsertMessage {
    String pair;
    BigDecimal price;
    BigDecimal number;
    long timestamp;
    public KLineInsertMessage(String pair,
                              BigDecimal price,
                              BigDecimal number,
                              long timestamp){
        this.pair = pair;
        this.price = price;
        this.number = number;
        this.timestamp = timestamp;
    }
}
