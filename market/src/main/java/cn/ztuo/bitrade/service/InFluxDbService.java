package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.component.KlinesData;
import cn.ztuo.bitrade.component.PriceData;
import cn.ztuo.bitrade.component.TrendencyData;
import cn.ztuo.bitrade.component.VolData;
import cn.ztuo.bitrade.entity.KLine;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
 * @author:wx
 * @description:
 * @create:2018-08-10  23:33
 */
@Service
public class InFluxDbService {
    @Value(value = "${influx.host}")
    String host;
    @Value(value = "${influx.database}")
    String database;
    volatile InfluxDB influxDB;


    InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused

    InfluxDB getInstance() {
        if (influxDB == null) {
            synchronized (InfluxDB.class){
                if(influxDB==null) {
                    influxDB = InfluxDBFactory.connect(host);
                }
            }
        }

        return influxDB;
    }

    public void initRetentionPolicy() {

        InfluxDB influxDB = getInstance();
        influxDB.createDatabase(database);
        influxDB.createRetentionPolicy("forever", database, "365d", "24h", 2, false);
        influxDB.createRetentionPolicy("6d", database, "6d", "24h", 2, true);
        influxDB.createRetentionPolicy("30d", database, "30d", "24h", 2, true);
        influxDB.createRetentionPolicy("365d", database, "365d", "24h", 2, true);


    }

    public void insertValue(String code, KLine kline) {
        Point point = Point.measurement("kline_new")
                .time(kline.getTime(), TimeUnit.MILLISECONDS)
                .tag("code", code)
                .addField("open", kline.getOpenPrice())
                .addField("high", kline.getHighestPrice())
                .addField("low", kline.getLowestPrice())
                .addField("close", kline.getClosePrice())
                .addField("vol", kline.getVolume())
                .build();
        getInstance().write(database, "forever", point);
    }


    public List<KLine> queryKline(String code, Long start, Long end, String sample, int limit) {
        start = start * 1000000;
        end = end * 1000000;
        String queryStr = String.format("select FIRST(open),MAX(high),MIN(low),LAST(close),SUM(vol) from kline_new where code='%s' and time >= %d and time < %d GROUP BY time(%s) order by time desc limit %d", code, start,end, sample, limit);
        return getNumbers(queryStr);
    }

    public static void main(String[] args) {
        InFluxDbService service = new InFluxDbService();
        service.database = "exchange";
        service.host = "http://localhost:8086";
     service.initRetentionPolicy();
        List<KLine> eos_usdt = service.queryKline("BTC/USDT", 0l, System.currentTimeMillis(), "15m", 200);
        eos_usdt.clear();;


    }

    private List<KLine> getNumbers(String queryStr) {
        List<KLine> list = new ArrayList<>();
        Query query = new Query(queryStr, database);
        QueryResult result = getInstance().query(query);
        List<KlinesData> klinesDataList = resultMapper.toPOJO(result, KlinesData.class);
        KlinesData lastData = null;
        for (KlinesData data : klinesDataList) {
            KLine kline = new KLine();
            if (data.getFirst() > 0){
                lastData = data;
            }
            if (lastData != null && data.getFirst() <= 0){
                kline.setTime(data.getTime().toEpochMilli());
                kline.setOpenPrice(new BigDecimal(lastData.getLast()));
                kline.setHighestPrice(new BigDecimal(lastData.getLast()));
                kline.setClosePrice(new BigDecimal(lastData.getLast()));
                kline.setLowestPrice(new BigDecimal(lastData.getLast()));
                list.add(kline);
            } else if (lastData != null){
                kline.setTime(data.getTime().toEpochMilli());
                kline.setOpenPrice(new BigDecimal(data.getFirst()));
                kline.setHighestPrice(new BigDecimal(data.getMax()));
                kline.setClosePrice(new BigDecimal(data.getLast()));
                kline.setLowestPrice(new BigDecimal(data.getMin()));
                kline.setVolume(new BigDecimal(data.getSum()));
                list.add(kline);
            }
        }
        return list;
    }


    @Cached(cacheType = CacheType.LOCAL, expire = 30,timeUnit = TimeUnit.MINUTES)
    public List<Number> getTendency(String code) {
        String queryStr = String.format("select LAST(close) from kline_new where code='%s' and time > now() - 28h GROUP BY time(1h)", code);
        Query query = new Query(queryStr, database);
        QueryResult result = getInstance().query(query);
        List<TrendencyData> tendencyDataList = resultMapper.toPOJO(result, TrendencyData.class);
        List<Number> list = new ArrayList<>();
        double lastData = 0;
        for (TrendencyData data : tendencyDataList) {
            if (data.getLast() != null){
                lastData = data.getLast();
            }
            list.add(lastData);
        }
        return list;

    }

    /**
     * 当前价格
     * @param pair
     * @return
     */
    public BigDecimal getMarketPrice(String pair){
        String queryStr = String.format("select close from kline_new where code='%s' order by time desc limit 1",pair);
        Query query = new Query(queryStr, database);
        QueryResult result = getInstance().query(query);
        List<PriceData> marketPrice = resultMapper.toPOJO(result, PriceData.class);
        if (marketPrice.size() > 0) return new BigDecimal(marketPrice.get(0).getClose());
        return BigDecimal.ZERO;
    }
    /**
     * 昨日价格
     * @param pair
     * @return
     */
    public BigDecimal getYesterdayPrice(String pair){
        String queryStr = String.format("select last(close) from kline_new where code='%s' and time > now() - 1d group by time(1d) order by time asc limit 1",pair);
        Query query = new Query(queryStr, database);
        QueryResult result = getInstance().query(query);
        List<TrendencyData> marketPrice = resultMapper.toPOJO(result, TrendencyData.class);
        if (marketPrice == null) return BigDecimal.ZERO;
        if (marketPrice.size() > 0) {
            TrendencyData data = marketPrice.get(0);
            if (data == null || data.getLast() == null) return BigDecimal.ZERO;
            return new BigDecimal(data.getLast());
        }
        return BigDecimal.ZERO;
    }


    public BigDecimal getVol24H(String pair) {
        String queryStr = String.format(" select sum(vol) from kline_new where code='%s' and time > now() - 1d group by time(1d) order by time desc limit 1",pair);
        Query query = new Query(queryStr, database);
        QueryResult result = getInstance().query(query);
        List<VolData> marketPrice = resultMapper.toPOJO(result, VolData.class);
        if (marketPrice.size() > 0) return new BigDecimal(marketPrice.get(0).getSum());
        return BigDecimal.ZERO;
    }


}
