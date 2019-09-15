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
    Map<String, BatchBlockQuque<Object>> queueMap = new ConcurrentHashMap<>();

    ExecutorService executorService=Executors.newFixedThreadPool(200);



    void putMessage(final String pair, Object message) {

        if (!queueMap.containsKey(pair)) {
            synchronized (ClearHandlerService.class){//线程安全
                if (!queueMap.containsKey(pair)) {
                    queueMap.put(pair, new BatchBlockQuque<>());
                    Handler handler=new Handler();
                    handler.batchBlockQuque= queueMap.get(pair);
                    executorService.submit(handler);
                }
            }

        }
        BatchBlockQuque<Object> quque = queueMap.get(pair);
        quque.putMessage(message);
    }



    public class Handler implements Runnable {
        public volatile BatchBlockQuque<Object> batchBlockQuque;


        @Override
        public void run() {
            while (true) {
                try {
                    List<Object> processTradeMessages = batchBlockQuque.getMessage();
                    batchUpdateService.mergeAndUpdate(processTradeMessages);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }

        }




    }


}
