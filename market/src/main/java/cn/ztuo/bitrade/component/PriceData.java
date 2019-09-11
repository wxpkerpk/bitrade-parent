package cn.ztuo.bitrade.component;

import lombok.Data;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

/*
 * @author:wx
 * @description:
 * @create:2018-09-05  19:06
 */

@Measurement(name = "kline_new")
@Data
public class PriceData {
    @Column(name="close")
    Double close;
}


