package cn.ztuo.bitrade.service;

import cn.ztuo.bitrade.component.KLineInsertMessage;
import cn.ztuo.bitrade.entity.KLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class KlineService {

    @Autowired
    InFluxDbService inFluxDbService;
    public final Map<String, KlineUtil> klineUtils = new ConcurrentHashMap<>();


    public BigDecimal getMarketPrice(String pair){
        KlineUtil klineUtil = klineUtils.get(pair);
        if (klineUtil == null) return BigDecimal.ZERO;
        KLine kline = klineUtil.KLines.get(KlineUtil.day_1);
        return kline.getClosePrice();
    }


    public List<Number> getTendency(String code) {
        return inFluxDbService.getTendency(code);
    }


        public void doKlineInsert(KLineInsertMessage msg) {
        KlineUtil klineUtil = klineUtils.get(msg.getPair());
        KLine isNeedSave = klineUtil.put(msg.getPrice(), msg.getNumber(), msg.getTimestamp());
        if (isNeedSave != null){
            inFluxDbService.insertValue(msg.getPair(),isNeedSave);
        }
    }

    private static long getTypeStartTime(long time,long typeTime){
        return time - (time % typeTime);
    }

    private static long getTypeEndTime(long time,long typeTime){
        long l = time % typeTime;
        if (l == 0) return time;
        return time + (typeTime - l);
    }

    public List<KLine> kline(String pair, Long start, Long end, Integer limit, String type) {
        if (limit == null || limit > 2000) limit = 2000;

        if (end == null || end ==0) end = System.currentTimeMillis();
        if (start == null || start ==0) start = end - limit * KlineUtil.klineTypes.get(type);


        List<KLine> kline = inFluxDbService.queryKline(pair, start, end, type, limit);
        /*
         * 坑爹啊,卧槽,缓存数据,命中缓存取出来的数据是反向的
         */
        kline = new ArrayList<>(kline);
        KLine lastKline = getLastKline(pair, type);
        if (kline.size() > 0){
            KLine numbers = kline.get(0);
            long perTime = numbers.getTime();
            BigDecimal close = numbers.getClosePrice();
            Long s = KlineUtil.klineTypes.get(type);
            // 内存时间是在时间内的
            if (lastKline!=null&&lastKline.getTime() >= start && lastKline.getTime() <= end){
                // 内存时间 != k 线最新时间
                if (lastKline.getTime() != perTime ){
                    while (perTime + s < lastKline.getTime()){
                        perTime = perTime + s;
                        kline.add(new KLine(type,close,close,close,close,perTime));
                    }
                    kline.add(0,lastKline);
                } else {
                    // 删除 k 先最新,补充成内存
                    kline.remove(0);
                    kline.add(0,lastKline);
                }
            }
        } else {
            if (lastKline!=null&&lastKline.getTime() > start && lastKline.getTime() < end){
                kline.add(0,lastKline);
            }
        }
        kline.sort(Comparator.comparingLong(o -> (long) o.getTime()));
        return kline;
    }

    public KLine getLastKline(String pair, String klineType){
        KlineUtil klineUtil = klineUtils.get(pair);
        if (klineUtil == null) return null;
        KLine kline = klineUtil.KLines.get(klineType);
        return kline;
    }

}
