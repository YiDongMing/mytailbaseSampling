package com.ydm.tailbase.bankendprocess;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ydm.tailbase.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class BackendServiceImpl implements BackendService{
    private static final Logger LOGGER = LoggerFactory.getLogger(BackendServiceImpl.class.getName());
    private static int BATCH_COUNT = 2000;
    public static List<TraceIdBatch> TRACEID_BATCH_LIST= new ArrayList<>();
    public static  void init() {
        for (int i = 0; i < BATCH_COUNT; i++) {
            TRACEID_BATCH_LIST.add(new TraceIdBatch());
        }
    }
    public static volatile Integer dealBatchPos1 = 0;
    public static volatile Integer dealBatchPos2 = 0;
    private static int startFlag = 0;

    @Override
    public void setWrongTraceId(String traceIdListJson, int batchPos,String port) {
        List<String> traceIdList = JSON.parseObject(traceIdListJson, new TypeReference<List<String>>() {
        });
        TraceIdBatch traceIdBatch = TRACEID_BATCH_LIST.get(batchPos);
        if (traceIdList != null && traceIdList.size() > 0) {
            traceIdBatch.setBatchPos(batchPos);
            traceIdBatch.setProcessCount(traceIdBatch.getProcessCount() + 1);
            traceIdBatch.getTraceIdList().addAll(traceIdList);
            if("8000".equals(port)){
                dealBatchPos1 = batchPos;
                traceIdBatch.setPort("8001");
            }else{
                dealBatchPos2 = batchPos;
                traceIdBatch.setPort("8000");
            }
        }
    }

    @Override
    public void setWrongTraceToMap(Map<String, List<String>> processMap,int batchPos) {
        /*for (Map.Entry<String, List<String>> entry : processMap.entrySet()) {
            String traceId = entry.getKey();
            Set<String> spanSet = CheckSumService.TRACE_CHUKSUM_MAP_TMP.get(traceId);
            if (spanSet == null) {
                spanSet = new HashSet<>();
                CheckSumService.TRACE_CHUKSUM_MAP_TMP.put(traceId, spanSet);
            }
            spanSet.addAll(entry.getValue());
        }*/
    }
    /**
     * get finished bath when current and next batch has all finished
     * @return
     */
    public static TraceIdBatch getFinishedBatch() {
        if(BackendController.FINISH_PROCESS_COUNT >= Constants.PROCESS_COUNT){
            for (int i = startFlag; i < BATCH_COUNT; i++) {
                TraceIdBatch currentBatch = TRACEID_BATCH_LIST.get(i);
                // when client process is finished, or then next trace batch is finished. to get checksum for wrong traces.
                if (currentBatch.getBatchPos() > 0) {
                    // reset
                    TraceIdBatch newTraceIdBatch = new TraceIdBatch();
                    BackendServiceImpl.TRACEID_BATCH_LIST.set(i, newTraceIdBatch);
                    startFlag = i;
                    return currentBatch;
                }
            }
        }
        for (int i = startFlag; i < BATCH_COUNT; i++) {
            TraceIdBatch currentBatch = TRACEID_BATCH_LIST.get(i);
            // when client process is finished, or then next trace batch is finished. to get checksum for wrong traces.
            if (currentBatch.getBatchPos() > 0 && BackendServiceImpl.dealBatchPos1-1>i && BackendServiceImpl.dealBatchPos2-1 >i) {
                // reset
                TraceIdBatch newTraceIdBatch = new TraceIdBatch();
                BackendServiceImpl.TRACEID_BATCH_LIST.set(i, newTraceIdBatch);
                startFlag = i;
                return currentBatch;
            }
        }
        return null;

    }
}
