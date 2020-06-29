package com.ydm.tailbase.bankendprocess;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ydm.tailbase.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
public class BackendController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendController.class.getName());

    // FINISH_PROCESS_COUNT will add one, when process call finish();
    public static volatile Integer FINISH_PROCESS_COUNT = 0;
    // save 90 batch for wrong trace
    private static int BATCH_COUNT = 2000;
    public static volatile Integer dealBatchPos1 = 0;
    public static volatile Integer dealBatchPos2 = 0;
    public static List<TraceIdBatch> TRACEID_BATCH_LIST= new ArrayList<>();
    public static  void init() {
        for (int i = 0; i < BATCH_COUNT; i++) {
            TRACEID_BATCH_LIST.add(new TraceIdBatch());
        }
    }

    private static volatile int startFlag = 0;
    private static int finishFlag = 0;

    @RequestMapping("setWrongTraceId")
    public void setWrongTraceId(@RequestParam String traceIdListJson, @RequestParam int batchPos, @RequestParam String port){
        List<String> traceIdList = JSON.parseObject(traceIdListJson, new TypeReference<List<String>>() {
        });
        TraceIdBatch traceIdBatch = TRACEID_BATCH_LIST.get(batchPos);
        if (traceIdList != null && traceIdList.size() > 0) {
            traceIdBatch.setBatchPos(batchPos);
            traceIdBatch.setProcessCount(traceIdBatch.getProcessCount() + 1);
            traceIdBatch.getTraceIdList().addAll(traceIdList);
        }
        if("8000".equals(port)){
            dealBatchPos1 = batchPos;
            traceIdBatch.setPort("8001");
        }else{
            dealBatchPos2 = batchPos;
            traceIdBatch.setPort("8000");
        }
    }

    @RequestMapping("/finish")
    public String finish() {
        FINISH_PROCESS_COUNT++;
        LOGGER.warn("receive call 'finish', count:" + FINISH_PROCESS_COUNT);
        return "suc";
    }

    /**
     * trace batch will be finished, when client process has finished.(FINISH_PROCESS_COUNT == PROCESS_COUNT)
     * @return
     */
    public static boolean isFinished() {
        if (FINISH_PROCESS_COUNT < Constants.PROCESS_COUNT) {
            return false;
        }
        if(finishFlag > 200){
            return true;
        }
        for (int i = 0; i < BATCH_COUNT; i++) {

            TraceIdBatch currentBatch = TRACEID_BATCH_LIST.get(i);
            if (currentBatch.getBatchPos() != 0) {
                return false;
            }
        }
        return true;
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
                    TRACEID_BATCH_LIST.set(i, newTraceIdBatch);
                    startFlag = i;
                    return currentBatch;
                }
            }
        }
        for (int i = startFlag; i < BATCH_COUNT; i++) {
            TraceIdBatch currentBatch = TRACEID_BATCH_LIST.get(i);
            // when client process is finished, or then next trace batch is finished. to get checksum for wrong traces.
            if (currentBatch.getBatchPos() > 0 && dealBatchPos1-1>i && dealBatchPos2-1 >i) {
                // reset
                TraceIdBatch newTraceIdBatch = new TraceIdBatch();
                BackendController.TRACEID_BATCH_LIST.set(i, newTraceIdBatch);
                startFlag = i;
                return currentBatch;
            }
        }
        return null;

    }

    @RequestMapping("/sendBlockFlag")
    public void sendBlockFlag( @RequestParam String  port){
        if("8000".equals(port)){
            CheckSumService.client1block = 1;
        }else{
            CheckSumService.client2block = 1;
        }
    }


}
