package com.ydm.tailbase.bankendprocess;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;


public interface BackendService {
    public void setWrongTraceId(String traceIdListJson,int batchPos);
}
