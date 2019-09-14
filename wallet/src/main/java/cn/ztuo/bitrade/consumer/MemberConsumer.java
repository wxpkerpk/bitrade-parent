package cn.ztuo.bitrade.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.ztuo.bitrade.constant.ActivityRewardType;
import cn.ztuo.bitrade.constant.BooleanEnum;
import cn.ztuo.bitrade.constant.RewardRecordType;
import cn.ztuo.bitrade.entity.*;
import cn.ztuo.bitrade.service.*;
import cn.ztuo.bitrade.util.GeneratorUtil;
import cn.ztuo.bitrade.util.MessageResult;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Component
public class MemberConsumer {
    private Logger logger = LoggerFactory.getLogger(MemberConsumer.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CoinService coinService;
    @Autowired
    private MemberWalletService memberWalletService;
    @Autowired
    private RewardActivitySettingService rewardActivitySettingService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private RewardRecordService rewardRecordService;
    @Autowired
    private ReleaseBalanceService releaseBalanceService;

    /**
     * 重置用户钱包地址
     *
     * @param record
     */
    @KafkaListener(topics = {"reset-member-address"})
    public void resetAddress(ConsumerRecord<String, String> record) {
        logger.info("handle member-register,key={},value={}", record.key(), record.value());
        String content = record.value();
        JSONObject json = JSON.parseObject(content);
        Coin coin = coinService.findByUnit(record.key());
        Assert.notNull(coin, "coin null");
        if (coin.getEnableRpc() == BooleanEnum.IS_TRUE) {
            MemberWallet memberWallet = memberWalletService.findByCoinUnitAndMemberId(record.key(), json.getLong
                    ("uid"));
            Assert.notNull(memberWallet, "wallet null");
            String account = "U" + json.getLong("uid") + GeneratorUtil.getNonceString(4);
            //远程RPC服务URL,后缀为币种单位
            String serviceName = "SERVICE-RPC-" + coin.getUnit();
            try {
                String url = "http://" + serviceName + "/rpc/address/{account}";
                ResponseEntity<MessageResult> result = restTemplate.getForEntity(url, MessageResult.class, account);
                logger.info("remote call:service={},result={}", serviceName, result);
                if (result.getStatusCode().value() == 200) {
                    MessageResult mr = result.getBody();
                    if (mr.getCode() == 0) {
                        String address = mr.getData().toString();
                        memberWallet.setAddress(address);
                    }
                }
            } catch (Exception e) {
                logger.error("call {} failed,error={}", serviceName, e.getMessage());
            }
            memberWalletService.save(memberWallet);
        }

    }



}

