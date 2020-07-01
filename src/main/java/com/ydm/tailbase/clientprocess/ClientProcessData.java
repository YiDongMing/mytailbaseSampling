package com.ydm.tailbase.clientprocess;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ydm.tailbase.CommonController;
import com.ydm.tailbase.Constants;
import com.ydm.tailbase.Util;
import com.ydm.tailbase.bankendprocess.BackendService;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class ClientProcessData implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProcessData.class.getName());

    /*ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:dubbo-consumer.xml");
    BackendService backendService = ctx.getBean("backendService", BackendService.class);*/

    // an list of trace map,like ring buffe.  key is traceId, value is spans ,  r
    private static List<ConcurrentHashMap<String, List<String>>> BATCH_TRACE_LIST = new ArrayList<>();
    // make 50 bucket to cache traceData
    private static int BATCH_COUNT = 1500;

    private static int STOP_COUNT = 300;

    private static int READ_POOL_SIZE = 2;

    private static int READ_MAX_SIZE = 4;

    public static int THREAD_COUNT = 4;

    public static volatile  Integer dealFlag = 1;

    private static Map<Integer,Set<String>> wrongMap = new ConcurrentHashMap<>(2000);

    public static List<SendInfo> failList = new ArrayList();


    public static  void init() {
        for (int i = 0; i < BATCH_COUNT; i++) {
            BATCH_TRACE_LIST.add(new ConcurrentHashMap<>(Constants.BATCH_SIZE));
        }
        for (int i = 0; i < 30; i++) {
            failList.add(new SendInfo());
        }
    }

    public static void start() {
        Thread t = new Thread(new ClientProcessData(), "ProcessDataThread");
        t.start();
    }
    private static ExecutorService readFileThreadPool = new ThreadPoolExecutor(READ_POOL_SIZE,READ_MAX_SIZE,60, TimeUnit.SECONDS,new LinkedBlockingQueue());



    @Override
    public void run() {
        Set<String> badTraceIdList = new HashSet<>(1000);
        try {
            String path = getPath();
            // process data on client, not server
            if (StringUtils.isEmpty(path)) {
                LOGGER.warn("path is empty");
                return;
            }
            URL url = new URL(path);
            HttpURLConnection httpURLConnection =(HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            int contentLength = httpURLConnection.getContentLength();
            LOGGER.info("contentLength:::::::::"+contentLength);
            httpURLConnection.connect();
            InputStream input = httpURLConnection.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(input));
            String line;
            long count = 0;
            int batchPos = 0;
            ConcurrentHashMap<String, List<String>> traceMap = BATCH_TRACE_LIST.get(batchPos);
            while ((line = bf.readLine()) != null) {
                count++;
                String[] cols = line.split("\\|");
                if (cols != null && cols.length > 1 ) {
                    String traceId = cols[0];
                    List<String> spanList = traceMap.get(traceId);
                    if (spanList == null) {
                        spanList = new ArrayList<>();
                        traceMap.put(traceId, spanList);
                    }
                    if("31c03d4774053b5f".equals(traceId) || "6446038004232f58".equals(traceId) || "4afbb32814c9502c".equals(traceId)){
                        LOGGER.info("contentLength"+traceId+"|"+batchPos);
                    }
                    spanList.add(line);
                    if (cols.length > 8) {
                        String tags = cols[8];
                        if (tags != null) {
                            if (tags.contains("error=1")) {
                                badTraceIdList.add(traceId);
                            } else if (tags.contains("http.status_code=") && tags.indexOf("http.status_code=200") < 0) {
                                badTraceIdList.add(traceId);
                            }
                        }
                    }
                }
                if (count % Constants.BATCH_SIZE == 0) {
                    batchPos = (int) count / Constants.BATCH_SIZE - 1;
                    traceMap = BATCH_TRACE_LIST.get(batchPos+1);
                    // batchPos begin from 0, so need to minus 1
                    // donot produce data, wait backend to consume data
                    // TODO to use lock/notify
                    long startFlag = System.currentTimeMillis();
                    while (true) {
                        if((batchPos - dealFlag) < STOP_COUNT){//说明收集错误数据的速度跟上拉取数据的速度了
                            //LOGGER.info("break:::::::"+batchPos + "|"+ dealFlag);
                            //BATCH_TRACE_LIST.get(pos).clear();
                            break;
                        }
                        //Thread.sleep(10);
                        //超时后则继续处理
                        long breakFlag = System.currentTimeMillis();
                        if((breakFlag -startFlag)>10000){
                            LOGGER.info("break of time:::::::"+batchPos+"|"+dealFlag);
                            //BATCH_TRACE_LIST.get(pos).clear();
                            break;
                        }
                    }
                    // batchPos begin from 0, so need to minus 1
                    if(badTraceIdList.size() > 0){
                        String json = JSON.toJSONString(badTraceIdList);
                        updateWrongTraceId(json, batchPos,false);
                        badTraceIdList.clear();
                    }
                }
            }
            LOGGER.info("finally count over !!!!!!!!!!!!!!!!!");
            if(badTraceIdList.size() > 0){
                String json = JSON.toJSONString(badTraceIdList);
                updateWrongTraceId(json, (int) (count / Constants.BATCH_SIZE),true);
            }
            bf.close();
            input.close();
            callFinish();
        } catch (Exception e) {
            LOGGER.warn("fail to process data", e);
        }
    }

    /**
     *  call backend controller to update wrong tradeId list.
     * @param batchPos
     */
    private void updateWrongTraceId(String json, int batchPos,boolean finish) {
        String port = System.getProperty("server.port", "8080");
        try {
            RequestBody body = new FormBody.Builder()
                    .add("traceIdListJson", json).add("batchPos", batchPos + "").add("port",port).build();
            Request request = new Request.Builder().url("http://localhost:8002/setWrongTraceId").post(body).build();
            Response response = Util.callHttp(request);
            response.close();
        } catch (Exception e) {
            LOGGER.warn("fail to updateBadTraceId, json:" + json + ", batch:" + batchPos);
        }
        //客户端找出错误的span
        //wrongMap.put(batchPos,badTraceIdList);
        //mutilDeal(batchPos,finish);
    }

    private static void sendBlockFlag(){
        try {
            String port = System.getProperty("server.port", "8080");
            RequestBody body = new FormBody.Builder()
                    .add("port", port).build();
            Request request = new Request.Builder().url("http://localhost:%s/sendBlockFlag").post(body).build();
            Response response = Util.callHttp(request);
            response.close();
        } catch (Exception e) {
            LOGGER.warn("fail to callFinish");
        }
    }

    // notify backend process when client process has finished.
    private void callFinish() {
        try {
            Request request = new Request.Builder().url("http://localhost:8002/finish").build();
            Response response = Util.callHttp(request);
            response.close();
        } catch (Exception e) {
            LOGGER.warn("fail to callFinish");
        }
    }

    public static String getWrongTracing(String wrongTraceIdList, int batchPos) {
        /*LOGGER.info(String.format("getWrongTracing, batchPos:%d, wrongTraceIdList:\n %s" ,
                batchPos, wrongTraceIdList));*/
        List<String> traceIdList = JSON.parseObject(wrongTraceIdList, new TypeReference<List<String>>(){});
        Map<String,List<String>> wrongTraceMap = new HashMap<>();
        int previous = batchPos - 1;
        if(previous <= 0){
            previous = 0;
        }
        getWrongTraceWithBatch(previous, batchPos, traceIdList, wrongTraceMap);
        int next = batchPos + 1;
        getWrongTraceWithBatch(batchPos, batchPos, traceIdList,  wrongTraceMap);
        getWrongTraceWithBatch(next, batchPos, traceIdList, wrongTraceMap);
        // to clear spans, don't block client process thread. TODO to use lock/notify
        synchronized(BATCH_TRACE_LIST.get(previous)){
            BATCH_TRACE_LIST.get(previous).clear();
        }
        dealFlag = batchPos;
        return JSON.toJSONString(wrongTraceMap);
    }

    private static void getWrongTraceWithBatch(int batchPos, int pos,  List<String> traceIdList, Map<String,List<String>> wrongTraceMap) {
        // donot lock traceMap,  traceMap may be clear anytime.
        Map<String, List<String>> traceMap = BATCH_TRACE_LIST.get(batchPos);
        for (String traceId : traceIdList) {
            if("31c03d4774053b5f".equals(traceId) || "6446038004232f58".equals(traceId) || "4afbb32814c9502c".equals(traceId)){
                LOGGER.info("getWrongTraceWithBatch"+traceId+"|"+batchPos);
            }
            List<String> spanList = traceMap.get(traceId);
            if (spanList != null) {
                // one trace may cross to batch (e.g batch size 20000, span1 in line 19999, span2 in line 20001)
                List<String> existSpanList = wrongTraceMap.get(traceId);
                if (existSpanList != null) {
                    existSpanList.addAll(spanList);
                } else {
                    wrongTraceMap.put(traceId, spanList);
                }
                // output spanlist to check
                //String spanListString = spanList.stream().collect(Collectors.joining("\n"));
                /*LOGGER.info(String.format("getWrongTracing, batchPos:%d, pos:%d, traceId:%s",
                        batchPos, pos,  traceId));*/
            }
        }
    }

    private static void sendWrongTracingToBackend(int batchPos){

    }

    private static Map<String,List<String>> getWrongTracingByClient(Set<String> badTraceIdList, int batchPos){
        Map<String,List<String>> wrongTraceMap = new HashMap<>();
        int pos = batchPos % BATCH_COUNT;
        int previous = pos - 1;
        if (previous == -1) {
            previous = BATCH_COUNT -1;
        }
        int next = pos + 1;
        if (next == BATCH_COUNT) {
            next = 0;
        }
        getWrongTraceWithBatchByClient(previous, pos, badTraceIdList, wrongTraceMap);
        getWrongTraceWithBatchByClient(pos, pos, badTraceIdList,  wrongTraceMap);
        getWrongTraceWithBatchByClient(next, pos, badTraceIdList, wrongTraceMap);

        return wrongTraceMap;
    }
    private static void getWrongTraceWithBatchByClient(int batchPos, int pos,  Set<String> traceIdList, Map<String,List<String>> wrongTraceMap) {
        // donot lock traceMap,  traceMap may be clear anytime.
        Map<String, List<String>> traceMap = BATCH_TRACE_LIST.get(batchPos);
        for (String traceId : traceIdList) {
            List<String> spanList = traceMap.get(traceId);
            if (spanList != null) {
                // one trace may cross to batch (e.g batch size 20000, span1 in line 19999, span2 in line 20001)
                List<String> existSpanList = wrongTraceMap.get(traceId);
                if (existSpanList != null) {
                    existSpanList.addAll(spanList);
                } else {
                    wrongTraceMap.put(traceId, spanList);
                }
            }
        }
    }

    private String getPath(){
        String port = System.getProperty("server.port", "8080");
        if ("8000".equals(port)) {
            return "http://localhost:" + CommonController.getDataSourcePort() + "/trace1.data";
        } else if ("8001".equals(port)){
            return "http://localhost:" + CommonController.getDataSourcePort() + "/trace2.data";
        } else {
            return null;
        }
    }
    /*private void mutilDeal(int batchPos,boolean finish){
        Runnable task = new Runnable(){
            @Override
            public void run() {
                if(batchPos !=0){//第一次批次不处理
                    if(finish){
                        Map<String, List<String>> wrongTracingByClient = getWrongTracingByClient(wrongMap.get(batchPos), batchPos);
                        try{
                            backendService.setWrongTraceToMap(wrongTracingByClient,batchPos);
                            LOGGER.info("suc to send last BadTraceId, batchPos:" + batchPos);
                        }catch (Exception e){
                            LOGGER.info("send wrong trace fail！！！");
                        }
                    }else{
                        if(wrongMap.get(batchPos-1) != null){//说明前一批次有错误的链路数据，需要查询出来
                            Map<String, List<String>> wrongTracingByClient = getWrongTracingByClient(wrongMap.get(batchPos - 1), batchPos -1);
                            try{
                                backendService.setWrongTraceToMap(wrongTracingByClient,batchPos-1);
                                //LOGGER.info("suc to sendBadTraceId, batchPos:" + (batchPos-1));
                            }catch (Exception e){
                                LOGGER.info("send wrong trace fail！！！");
                            }
                        }
                    }
                }
            }
        };
        readFileThreadPool.execute(task);
    }*/

    /*private  void dealFailList(String json,int batchPos,String port){
        Runnable dealTask = new Runnable() {
            @Override
            public void run() {
                boolean flag = true;
                while (flag){
                    try {
                        backendService.setWrongTraceId(json,batchPos,port);
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    flag = false;
                }
            }
        };
        readFileThreadPool.execute(dealTask);
    }*/

    /*public BackendService getBackendService() {
        return backendService;
    }

    public void setBackendService(BackendService backendService) {
        this.backendService = backendService;
    }*/
}
