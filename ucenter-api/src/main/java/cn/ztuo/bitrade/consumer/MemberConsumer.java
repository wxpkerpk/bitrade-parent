package cn.ztuo.bitrade.consumer;

import cn.ztuo.bitrade.constant.ActivityRewardType;
import cn.ztuo.bitrade.constant.RewardRecordType;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.CoinService;
import cn.ztuo.bitrade.service.MemberWalletService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class MemberConsumer {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CoinService coinService;
    @Autowired
    private MemberWalletService memberWalletService;

    /**
     * 客户注册消息
     *
     * @param content
     */
    @KafkaListener(topics = {"member-register"})
    public void handle(String content) {
        if (StringUtils.isEmpty(content)) return;
        JSONObject json = JSON.parseObject(content);
        if (json == null) return;
        //获取所有支持的币种
        List<Coin> coins = coinService.findAll();
        for (Coin coin : coins) {
            MemberWallet wallet = new MemberWallet();
            wallet.setCoin(coin);
            wallet.setMemberId(json.getLong("uid"));
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozenBalance(BigDecimal.ZERO);
            wallet.setReleaseBalance(BigDecimal.ZERO);
            wallet.setAddress("");
            wallet.setCoinUnit(coin.getUnit());
            wallet.setId(wallet.makeId());
            //保存
            memberWalletService.save(wallet);
        }

    }
}
