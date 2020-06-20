package com.ydm.tailbase.bankendprocess;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


public class BackendServiceImpl implements BackendService{
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendServiceImpl.class.getName());
    private static int BATCH_COUNT = 90;
    public static List<TraceIdBatch> TRACEID_BATCH_LIST= new ArrayList<>();
    public static  void init() {
        for (int i = 0; i < BATCH_COUNT; i++) {
            TRACEID_BATCH_LIST.add(new TraceIdBatch());
        }
    }
    @Override
    public void setWrongTraceId(String traceIdListJson, int batchPos) {
        int pos = batchPos % BATCH_COUNT;
        List<String> traceIdList = JSON.parseObject(traceIdListJson, new TypeReference<List<String>>() {
        });
        TraceIdBatch traceIdBatch = TRACEID_BATCH_LIST.get(pos);
        if (traceIdBatch.getBatchPos() != 0 && traceIdBatch.getBatchPos() != batchPos) {
            LOGGER.warn("overwrite traceId batch when call setWrongTraceId");
        }

        if (traceIdList != null && traceIdList.size() > 0) {
            traceIdBatch.setBatchPos(batchPos);
            traceIdBatch.setProcessCount(traceIdBatch.getProcessCount() + 1);
            traceIdBatch.getTraceIdList().addAll(traceIdList);
        }
    }
}
