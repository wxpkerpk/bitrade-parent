package cn.ztuo.bitrade.job;

import cn.ztuo.bitrade.component.KLineInsertMessage;
import cn.ztuo.bitrade.processor.CoinProcessorFactory;
import cn.ztuo.bitrade.service.KlineService;
import cn.ztuo.bitrade.service.KlineUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * 生成各时间段的K线信息
 *
 */
@Component
@Slf4j
public class KLineGeneratorJob {
    @Autowired
    private CoinProcessorFactory processorFactory;

    public static Set<String> pairs=new HashSet<>();
    public void startKlineService(String pair) {
        pairs.add(pair);
        klineService.klineUtils.put(pair,new KlineUtil(pair));
    }
    @Autowired
    KlineService klineService;
    /**
     * 每分钟定时器，处理分钟K线
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void task(){
        long currentTimeMillis = System.currentTimeMillis();
        for (String pair : pairs) {
            BigDecimal marketPrice = klineService.getMarketPrice(pair);
            if (marketPrice.compareTo(BigDecimal.ZERO) != 0){
                klineService.doKlineInsert(new KLineInsertMessage(pair,marketPrice,BigDecimal.ZERO,currentTimeMillis));
            }
        }
    }
    @Scheduled(cron = "0 * * * * *")
    public void handle5minKLine(){
        Calendar calendar = Calendar.getInstance();
        log.info("分钟K线:{}",calendar.getTime());
        //将秒、微秒字段置为0
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        long time = calendar.getTimeInMillis();
        int minute = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        processorFactory.getProcessorMap().forEach((symbol,processor)->{

            processor.update24HVolume(time);
            if(hour == 0 && minute == 0){
                processor.resetThumb();
            }
        });
    }

}
