package test;

import cn.ztuo.bitrade.MarketApplication;
import cn.ztuo.bitrade.entity.BalanceItems;
import cn.ztuo.bitrade.service.BatchUpdateService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MarketApplication.class)
public class TestUpdate {


    @Autowired
    BatchUpdateService batchUpdateService;

    @Test
    public void test()
    {


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
          int i=batchUpdateService.updateWalletBalance(balanceItems);
        System.out.println("111111111111111111111");

    }

}
