package cn.ztuo.bitrade.service;


import cn.ztuo.bitrade.dao.ExchangeOrderDetailRepository;
import cn.ztuo.bitrade.dao.ExchangeOrderRepository;
import cn.ztuo.bitrade.dao.MemberTransactionDao;
import cn.ztuo.bitrade.dao.OrderDetailAggregationRepository;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.util.BatchBlockQuque;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ClearHandlerService {


    @Autowired
    BatchUpdateService batchUpdateService;
    Map<String, BatchBlockQuque<ProcessTradeMessage>> queueMap = new ConcurrentHashMap<>();

    ExecutorService executorService=Executors.newFixedThreadPool(200);



    void putMessage(final String pair, ProcessTradeMessage processTradeMessage) {

        if (!queueMap.containsKey(pair)) {
            queueMap.put(pair, new BatchBlockQuque<>());
            executorService.submit(new Handler());
        }
        BatchBlockQuque<ProcessTradeMessage> quque = queueMap.get(pair);
        quque.putMessage(processTradeMessage);
    }



    public class Handler implements Runnable {
        public BatchBlockQuque<ProcessTradeMessage> batchBlockQuque;


        @Override
        public void run() {
            while (true) {
                try {
                    List<ProcessTradeMessage> processTradeMessages = batchBlockQuque.getMessage();
                    batchUpdateService.mergeAndUpdate(processTradeMessages);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }

        }




    }


}
