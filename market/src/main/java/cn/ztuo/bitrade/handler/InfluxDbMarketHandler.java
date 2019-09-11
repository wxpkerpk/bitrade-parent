package cn.ztuo.bitrade.handler;

import cn.ztuo.bitrade.entity.CoinThumb;
import cn.ztuo.bitrade.entity.ExchangeTrade;
import cn.ztuo.bitrade.entity.KLine;
import cn.ztuo.bitrade.service.InFluxDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class InfluxDbMarketHandler implements MarketHandler {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    InFluxDbService inFluxDbService;
    public void handleTrade(String symbol, ExchangeTrade exchangeTrade, CoinThumb thumb) {
        mongoTemplate.insert(exchangeTrade, "exchange_trade_" + symbol);
    }

    @Override
    public void handleKLine(String symbol, KLine kLine) {
        inFluxDbService.insertValue(symbol,kLine);
        //   mongoTemplate.insert(kLine,"exchange_kline_"+symbol+"_"+kLine.getPeriod());
    }
}
