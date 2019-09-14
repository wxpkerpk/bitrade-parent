package cn.ztuo.bitrade.processor;


import cn.ztuo.bitrade.entity.ProcessOrderResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

@Service
public class CleanHanderService {



    String pair;
    LinkedBlockingQueue<ProcessOrderResult> resultQueue = new LinkedBlockingQueue<>();




}
