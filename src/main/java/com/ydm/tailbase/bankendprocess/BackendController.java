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
    private static volatile Integer FINISH_PROCESS_COUNT = 0;
    // save 90 batch for wrong trace
    private static int BATCH_COUNT = 90;


    @RequestMapping("setWrongTraceId")
    public String setWrongTraceId(@RequestParam String traceIdListJson, @RequestParam int batchPos){
        int pos = batchPos % BATCH_COUNT;
        List<String> traceIdList = JSON.parseObject(traceIdListJson, new TypeReference<List<String>>() {
        });
        LOGGER.info(String.format("setWrongTraceId had called, batchPos:%d, traceIdList:%s", batchPos, traceIdListJson));
        TraceIdBatch traceIdBatch = BackendServiceImpl.TRACEID_BATCH_LIST.get(pos);
        if (traceIdBatch.getBatchPos() != 0 && traceIdBatch.getBatchPos() != batchPos) {
            LOGGER.warn("overwrite traceId batch when call setWrongTraceId");
        }

        if (traceIdList != null && traceIdList.size() > 0) {
            traceIdBatch.setBatchPos(batchPos);
            traceIdBatch.setProcessCount(traceIdBatch.getProcessCount() + 1);
            traceIdBatch.getTraceIdList().addAll(traceIdList);
        }
        return "suc";
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
        for (int i = 0; i < BATCH_COUNT; i++) {

            TraceIdBatch currentBatch = BackendServiceImpl.TRACEID_BATCH_LIST.get(i);
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
        for (int i = 0; i < BATCH_COUNT; i++) {
            int next = i + 1;
            if (next >= BATCH_COUNT) {
                next = 0;
            }
            TraceIdBatch currentBatch = BackendServiceImpl.TRACEID_BATCH_LIST.get(i);
            TraceIdBatch nextBatch = BackendServiceImpl.TRACEID_BATCH_LIST.get(next);
            // when client process is finished, or then next trace batch is finished. to get checksum for wrong traces.
            if ((FINISH_PROCESS_COUNT >= Constants.PROCESS_COUNT && currentBatch.getBatchPos() > 0) ||
                    (nextBatch.getProcessCount() >= Constants.PROCESS_COUNT && currentBatch.getProcessCount() >= Constants.PROCESS_COUNT)) {
                // reset
                TraceIdBatch newTraceIdBatch = new TraceIdBatch();
                BackendServiceImpl.TRACEID_BATCH_LIST.set(i, newTraceIdBatch);
                return currentBatch;
            }
        }
        return null;
    }
}
