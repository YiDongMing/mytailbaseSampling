package com.ydm.tailbase.bankendprocess;

import com.ydm.tailbase.Constants;

import java.util.ArrayList;
import java.util.List;

public class TraceIdBatch {
    private int batchPos = 0;
    private int processCount = 0;
    private List<String> traceIdList = new ArrayList<>(Constants.BATCH_SIZE / 10);
    private String port = "0"; //标记需要去哪个客户端取数据，8000表示客户端1，8002表示客户端2，

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getBatchPos() {
        return batchPos;
    }

    public void setBatchPos(int batchPos) {
        this.batchPos = batchPos;
    }

    public int getProcessCount() {
        return processCount;
    }

    public void setProcessCount(int processCount) {
        this.processCount = processCount;
    }

    public List<String> getTraceIdList() {
        return traceIdList;
    }

    @Override
    public String toString() {
        return "TraceIdBatch{" +
                "batchPos=" + batchPos +
                ", processCount=" + processCount +
                ", port=" + port +
                '}';
    }
}
