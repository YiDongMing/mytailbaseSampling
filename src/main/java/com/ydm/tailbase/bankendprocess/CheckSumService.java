package com.ydm.tailbase.bankendprocess;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ydm.tailbase.CommonController;
import com.ydm.tailbase.Util;
import com.ydm.tailbase.clientprocess.ClientProcessData;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.ydm.tailbase.Constants.CLIENT_PROCESS_PORT1;
import static com.ydm.tailbase.Constants.CLIENT_PROCESS_PORT2;

public class CheckSumService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProcessData.class.getName());

    // save chuckSum for the total wrong trace
    private static Map<String, String> TRACE_CHUCKSUM_MAP= new ConcurrentHashMap<>();


    private static ExecutorService checkThreadPool = new ThreadPoolExecutor(2,3,60, TimeUnit.SECONDS,new LinkedBlockingQueue());
    private static ExecutorService MD5ThreadPool = new ThreadPoolExecutor(1,1,60, TimeUnit.SECONDS,new LinkedBlockingQueue());

    private static long  countTime = 0l;
    private static long  countTime2 = 0l;
    public static volatile int client1block = 0;
    public static volatile int client2block = 0;
    public static volatile int dealBatch = 0;
    public static volatile int MD5Batch = 0;
    public void start() {
        run();
    }


    public void run() {
        Runnable task = new Runnable(){
            @Override
            public void run() {
                TraceIdBatch traceIdBatch = null;
                String[] ports = new String[]{CLIENT_PROCESS_PORT1, CLIENT_PROCESS_PORT2};
                while (true) {
                    long startFlag = System.currentTimeMillis();
                    try {
                        long endFlag = System.currentTimeMillis();
                        if((endFlag-startFlag)>600000){
                            if(sendCheckSum()){
                                break;
                            }
                        }
                        traceIdBatch = BackendController.getFinishedBatch();
                        if (traceIdBatch == null) {
                            // send checksum when client process has all finished.
                            if (BackendController.isFinished()) {
                                //LOGGER.info("countTime:::::::::::"+countTime);
                                //LOGGER.info("countTime2:::::::::::"+countTime2);
                                MD5ThreadPool.shutdown();
                                while (true){
                                    //LOGGER.info("MD5ThreadPool not finish:::::::::::");
                                    if(MD5ThreadPool.isTerminated()){
                                        if (sendCheckSum()) {
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                            continue;
                        }
                        Map<String, Set<String>> map = new HashMap<>();
                        //long start = System.currentTimeMillis();
                        int batchPos = traceIdBatch.getBatchPos();
                        // to get all spans from remote
                        for (String port : ports) {
                            Map<String, List<String>> processMap =
                                    getWrongTrace(JSON.toJSONString(traceIdBatch.getTraceIdList()), port, batchPos);
                            if (processMap != null) {
                                for (Map.Entry<String, List<String>> entry : processMap.entrySet()) {
                                    String traceId = entry.getKey();
                                    Set<String> spanSet = map.get(traceId);
                                    if (spanSet == null) {
                                        spanSet = new HashSet<>();
                                        map.put(traceId, spanSet);
                                    }
                                    spanSet.addAll(entry.getValue());
                                }
                            }else{
                                //LOGGER.info("get traceIdBatch fail："+JSON.toJSONString(traceIdBatch.getTraceIdList()));
                            }
                        }
                        dealBatch = batchPos;
                        dealMD5(map);
                        //long end = System.currentTimeMillis();
                        //countTime = countTime + (end - start);
                    } catch (Exception e) {
                        // record batchPos when an exception  occurs.
                        int batchPos = 0;
                        if (traceIdBatch != null) {
                            batchPos = traceIdBatch.getBatchPos();
                        }
                        LOGGER.warn(String.format("fail to getWrongTrace, batchPos:%d", batchPos), e);
                    } finally {
                        if (traceIdBatch == null) {
                            try {
                                //Thread.sleep(100);
                            } catch (Throwable e) {
                                // quiet
                            }
                        }
                    }
                }
            }
        };
        checkThreadPool.execute(task);
    }

    public static void dealMD5(Map<String, Set<String>> map){
        Runnable MD5Task = new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
                    String traceId = entry.getKey();
                    Set<String> spanSet = entry.getValue();
                    // order span with startTime
                    String spans = spanSet.stream().sorted(
                            Comparator.comparing(CheckSumService::getStartTime)).collect(Collectors.joining("\n"));
                    spans = spans + "\n";
                    // output all span to check
                    // LOGGER.info("traceId:" + traceId + ",value:\n" + spans);
                    TRACE_CHUCKSUM_MAP.put(traceId, Util.MD5(spans));
                }
            }
        };
        MD5ThreadPool.execute(MD5Task);
    }

    public void sendDealFlag(String port){
        try {
            RequestBody body = new FormBody.Builder()
                    .add("dealFlag", "0").build();
            String url = String.format("http://localhost:%s/sendDealFlag", port);
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = Util.callHttp(request);
            response.close();
        } catch (Exception e) {
            LOGGER.warn("fail to sendDealFlag, dealFlag:" + BackendServiceImpl.dealBatchPos1.toString()+"|"+BackendServiceImpl.dealBatchPos2.toString(), e);
        }
    }

    /**
     * call client process, to get all spans of wrong traces.
     * @param traceIdList
     * @param port
     * @param batchPos
     * @return
     */
    private Map<String,List<String>>  getWrongTrace(@RequestParam String traceIdList, String port, int batchPos) {
        try {
            RequestBody body = new FormBody.Builder()
                    .add("traceIdList", traceIdList).add("batchPos", batchPos + "").build();
            String url = String.format("http://localhost:%s/getWrongTrace", port);
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = Util.callHttp(request);
            Map<String,List<String>> resultMap = JSON.parseObject(response.body().string(),
                    new TypeReference<Map<String, List<String>>>() {});
            response.close();
            return resultMap;
        } catch (Exception e) {
            LOGGER.warn("fail to getWrongTrace, json:" + traceIdList + ",batchPos:" + batchPos, e);
        }
        return null;
    }

    private boolean sendCheckSum() {
        try {
            String result = JSON.toJSONString(TRACE_CHUCKSUM_MAP);
            RequestBody body = new FormBody.Builder()
                    .add("result", result).build();
            String url = String.format("http://localhost:%s/api/finished", CommonController.getDataSourcePort());
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = Util.callHttp(request);
            if (response.isSuccessful()) {
                response.close();
                LOGGER.warn("suc to sendCheckSum, result:" + result );
                return true;
            }
            LOGGER.warn("fail to sendCheckSum:" + response.message());
            response.close();
            return false;
        } catch (Exception e) {
            LOGGER.warn("fail to call finish", e);
        }
        return false;
    }

    public static long getStartTime(String span) {
        if (span != null) {
            String[] cols = span.split("\\|");
            if (cols.length > 8) {
                return Util.toLong(cols[1], -1);
            }
        }
        return -1;
    }
}
