package com.ydm.tailbase.clientprocess;

import com.ydm.tailbase.bankendprocess.BackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ClientMemory implements Runnable{


    private static final Logger LOGGER = LoggerFactory.getLogger(ClientMemory.class.getName());

    public static void start(){
        Thread t = new Thread(new ClientMemory(), "ClientMemoryThread");
        t.start();
    }
    @Override
    public void run() {
        /*ClientProcessData data = new ClientProcessData();
        while (true){
            for(int i=0; i<ClientProcessData.failList.size();i++){
                if(ClientProcessData.failList.get(i) != null){
                    SendInfo info = ClientProcessData.failList.get(i);
                    try {
                        data.getBackendService().setWrongTraceId(info.getJson(),info.getBatchPos(),info.getPort());
                        Thread.sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    info = null;
                }
            }
        }*/

    }
}
