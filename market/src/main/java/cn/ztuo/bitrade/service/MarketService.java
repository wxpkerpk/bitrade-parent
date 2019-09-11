package cn.ztuo.bitrade.service;


import cn.ztuo.bitrade.entity.KLine;
import cn.ztuo.bitrade.entity.ExchangeTrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MarketService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    KlineService klineService;
    public List<KLine> findAllKLine(String symbol,String peroid){
        Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC,"time"));
        Query query = new Query().with(sort).limit(1000);

        return mongoTemplate.find(query,KLine.class,"exchange_kline_"+symbol+"_"+peroid);
    }

    public List<KLine> findAllKLine(String symbol,long fromTime,long toTime,String period){
        if(period.endsWith("H") || period.endsWith("h")){
            period = period.substring(0,period.length()-1) + "h";
        }
        else if(period.endsWith("D") || period.endsWith("d")){
            period = period.substring(0,period.length()-1) + "d";
        }
        else if(period.endsWith("W") || period.endsWith("w")){
            period = period.substring(0,period.length()-1) + "w";
        }
        else if(period.endsWith("M") || period.endsWith("m")){
            period = period.substring(0,period.length()-1) + "m";
        } else {
            Integer val = Integer.parseInt(period);
            if (val < 60) {
                period = period + "m";
            } else {
                period = (val / 60) + "h";
            }
        }
        List<KLine> kLines =        klineService.kline(symbol,fromTime,toTime,2000,period);
        return kLines;
    }

    public ExchangeTrade findFirstTrade(String symbol,long fromTime,long toTime){
        Criteria criteria = Criteria.where("time").gte(fromTime).andOperator(Criteria.where("time").lte(toTime));
        Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC,"time"));
        Query query = new Query(criteria).with(sort);
        return mongoTemplate.findOne(query,ExchangeTrade.class,"exchange_trade_"+symbol);
    }

    public ExchangeTrade findLastTrade(String symbol,long fromTime,long toTime){
        Criteria criteria = Criteria.where("time").gte(fromTime).andOperator(Criteria.where("time").lte(toTime));
        Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC,"time"));
        Query query = new Query(criteria).with(sort);
        return mongoTemplate.findOne(query,ExchangeTrade.class,"exchange_trade_"+symbol);
    }

    public ExchangeTrade findTrade(String symbol, long fromTime, long toTime, Sort.Order order){
        Criteria criteria = Criteria.where("time").gte(fromTime).andOperator(Criteria.where("time").lte(toTime));
        Sort sort = new Sort(order);
        Query query = new Query(criteria).with(sort);
        return mongoTemplate.findOne(query,ExchangeTrade.class,"exchange_trade_"+symbol);
    }

    public List<ExchangeTrade> findTradeByTimeRange(String symbol, long timeStart, long timeEnd){
        Criteria criteria = Criteria.where("time").gte(timeStart).andOperator(Criteria.where("time").lt(timeEnd));
        Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC,"time"));
        Query query = new Query(criteria).with(sort);

        return mongoTemplate.find(query,ExchangeTrade.class,"exchange_trade_"+symbol);
    }

    public void saveKLine(String symbol,KLine kLine){
        if(kLine.getVolume().compareTo(BigDecimal.ZERO) > 0) {
            mongoTemplate.insert(kLine, "exchange_kline_" + symbol + "_" + kLine.getPeriod());
        }
    }

    /**
     * 查找某时间段内的交易量
     * @param symbol
     * @param timeStart
     * @param timeEnd
     * @return
     */
    public BigDecimal findTradeVolume(String symbol, long timeStart, long timeEnd){
        Criteria criteria = Criteria.where("time").gt(timeStart)
                .andOperator(Criteria.where("time").lte(timeEnd));
                //.andOperator(Criteria.where("volume").gt(0));
        Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC,"time"));
        Query query = new Query(criteria).with(sort);
        List<KLine> kLines =  mongoTemplate.find(query,KLine.class,"exchange_kline_"+symbol+"_1min");
        BigDecimal totalVolume = BigDecimal.ZERO;
        for(KLine kLine:kLines){
            totalVolume = totalVolume.add(kLine.getVolume());
        }
        return totalVolume;
    }
}
