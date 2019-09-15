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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BatchUpdateService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private OrderDetailAggregationRepository orderDetailAggregationRepository;
    @Autowired
    BatchUpdateService batchUpdateService;
    Map<String, BatchBlockQuque<ProcessTradeMessage>> queueMap = new ConcurrentHashMap<>();
    @Autowired
    private ExchangeOrderDetailRepository orderDetailRepository;

    @Autowired
    ExchangeOrderRepository exchangeOrderRepository;
    @Autowired
    MemberTransactionDao memberTransactionDao;

    @Transactional
    @Modifying
    public int updateWalletBalance(List<BalanceItems> items) {
        StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("update member_wallet  set balance = balance + ( case  id ");
        for (BalanceItems i : items) {
            if (i.getAvailableAmount() != null) {

                sqlBuilder.append("when ").append("'").append(i.getWalletId()).append("'")
                        .append(" ").append(" then ").append(i.getAvailableAmount()).append(" ");
            }

        }
        sqlBuilder.append(" end ), ");
        sqlBuilder.append(" frozen_balance = frozen_balance - ( case  id ");
        for (BalanceItems i : items) {
            if (i.getFrozenAmount() != null) {
                sqlBuilder.append("when ").append("'").append(i.getWalletId()).append("'")
                        .append(" ").append(" then ").append(i.getFrozenAmount()).append(" ");
            }

        }
        sqlBuilder.append(" end ) ");
        sqlBuilder.append("where id in (");

        for (BalanceItems i : items) {
            sqlBuilder.append("'").append(i.getWalletId()).append("'").append(",");
        }
        sqlBuilder.deleteCharAt(sqlBuilder.length()-1);
        sqlBuilder.append(" )");
        String sql = sqlBuilder.toString();
        int i = entityManager.createNativeQuery(sql).executeUpdate();
//         int i= sessionFactory.getCurrentSession().createSQLQuery(sql).executeUpdate();
        return i;
    }
    private void addLogs(List<MemberTransaction> transactions, List<OrderDetailAggregation> detailAggregations, List<ExchangeOrderDetail> details, ProcessTradeMessage message) {
        transactions.add(message.getBuyOrderProcessResult().getIncomeMemberTransaction());
        transactions.add(message.getBuyOrderProcessResult().getOutcomeMemberTransaction());
        transactions.add(message.getSellOrderProcessResult().getIncomeMemberTransaction());
        transactions.add(message.getSellOrderProcessResult().getOutcomeMemberTransaction());
        detailAggregations.add(message.getSellOrderProcessResult().getOrderDetailAggregation());
        detailAggregations.add(message.getBuyOrderProcessResult().getOrderDetailAggregation());

        details.add(message.getBuyOrderProcessResult().getExchangeOrderDetail());
        details.add(message.getSellOrderProcessResult().getExchangeOrderDetail());
    }

    private void mergeRefund(Map<String, BalanceItems> itemsMap, RefundItem item) {
        if (item != null&&item.getAmount()!=null&&item.getAmount().compareTo(BigDecimal.ZERO)>0) {
            String walletId = MemberWallet.makeWalletId(item.getCoinSymbol(), item.getUserId());
            BalanceItems balanceItems = itemsMap.getOrDefault(walletId, new BalanceItems());
            balanceItems.setWalletId(walletId);
            balanceItems.setFrozenAmount(balanceItems.getFrozenAmount().add(item.getAmount()));
            balanceItems.setAvailableAmount(balanceItems.getAvailableAmount().add(item.getAmount()));
            itemsMap.put(walletId,balanceItems);
        }
    }

    private void mergeBalance(Map<String, BalanceItems> itemsMap, long userId, ProcessOrderResult processOrderResult) {
        String outSymbol = processOrderResult.getOutcomeSymbol();
        String outWalletId = MemberWallet.makeWalletId(outSymbol, userId);
        String inSymbol1 = processOrderResult.getIncomeSymbol();
        String inWalletId = MemberWallet.makeWalletId(inSymbol1, userId);
        BalanceItems out = itemsMap.getOrDefault(outWalletId, new BalanceItems());
        out.setWalletId(outWalletId);
        out.setFrozenAmount(out.getFrozenAmount().add(processOrderResult.getOutcomeCoinAmount()));
        itemsMap.put(outWalletId, out);
        BalanceItems in = itemsMap.getOrDefault(inWalletId, new BalanceItems());
        in.setWalletId(inWalletId);
        in.setAvailableAmount(in.getAvailableAmount().add(processOrderResult.getIncomeCoinAmount()));
        itemsMap.put(inWalletId, in);
    }

    @Transactional
    @Modifying
    public void mergeAndUpdate( List<Object> processTradeMessages){

        Map<String, BalanceItems> itemsMap = new TreeMap<>();//有序
        List<ExchangeOrder> completedOrders = new ArrayList<>(processTradeMessages.size() * 2);
        List<MemberTransaction> transactions = new ArrayList<>(processTradeMessages.size() * 4);
        List<OrderDetailAggregation> detailAggregations = new ArrayList<>(processTradeMessages.size() * 2);
        List<ExchangeOrderDetail> details = new ArrayList<>(processTradeMessages.size() * 2);

        for (Object m : processTradeMessages) {

            if(m instanceof ProcessTradeMessage) {
                ProcessTradeMessage message= (ProcessTradeMessage) m;
                long userId = message.getBuyOrderProcessResult().getUserId();
                addLogs(transactions, detailAggregations, details, message);

                ProcessOrderResult processOrderResult = message.getBuyOrderProcessResult();
                mergeBalance(itemsMap, userId, processOrderResult);
                mergeBalance(itemsMap, message.getSellOrderProcessResult().getUserId(), message.getSellOrderProcessResult());
                RefundItem item = message.getBuyRefund();
                mergeRefund(itemsMap, item);
                mergeRefund(itemsMap, message.getSellRefund());
                if (message.getBuyRefund() != null && message.getBuyRefund().getOrder() != null)
                    completedOrders.add(message.getBuyRefund().getOrder());
                if (message.getSellRefund() != null && message.getSellRefund().getOrder() != null)
                    completedOrders.add(message.getSellRefund().getOrder());
            }else if(m instanceof RefundItem){
                RefundItem refundItem= (RefundItem) m;
                mergeRefund(itemsMap, refundItem);
                completedOrders.add(refundItem.getOrder());
            }
        }

        List<BalanceItems> balanceItems = new ArrayList<>(itemsMap.size());
        itemsMap.forEach((key, value) -> {
            balanceItems.add(value);
        });
        orderDetailRepository.save(details);
        memberTransactionDao.save(transactions);
        exchangeOrderRepository.save(completedOrders);
        orderDetailAggregationRepository.save(detailAggregations);


        batchUpdateService.updateWalletBalance(balanceItems);


    }

    public static void main(String[] s) {

        BalanceItems items1 = new BalanceItems();
        BalanceItems items2 = new BalanceItems();
        items1.setWalletId("BTC:1");
        items2.setWalletId("USDT:2");
        items1.setAvailableAmount(BigDecimal.ONE);
        items1.setFrozenAmount(BigDecimal.TEN);
        items2.setAvailableAmount(BigDecimal.ONE);
        items2.setFrozenAmount(BigDecimal.TEN);
        List<BalanceItems> balanceItems = new ArrayList<>();
        balanceItems.add(items1);
        balanceItems.add(items2);
        new BatchUpdateService().updateWalletBalance(balanceItems);
        System.out.println(2);


    }


}
