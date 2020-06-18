package com.ydm.tailbase.bankendprocess;

import com.ydm.tailbase.Constants;
/*import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;*/

public class BackendProvider {

    /*public static void init(){
        ApplicationConfig application = new ApplicationConfig();
        application.setName("dubbo-api-test");

        // 连接注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setProtocol("simple");
        registry.setAddress("127.0.0.1:80");
        registry.setRegister(false);
        registry.setCheck(false);

        // 服务提供者协议配置
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName("dubbo");
        protocol.setPort(Integer.valueOf(Constants.BACKEND_PROCESS_PORT2));
        protocol.setThreads(100);

        // 注意：ServiceConfig为重对象，内部封装了与注册中心的连接，以及开启服务端口
        // 服务提供者暴露服务配置
        BackendService backendService = new BackendServiceImpl();
        ServiceConfig<BackendService> service = new ServiceConfig<BackendService>(); // 此实例很重，封装了与注册中心的连接，请自行缓存，否则可能造成内存和连接泄漏
        service.setApplication(application);
        service.setRegistry(registry); // 多个注册中心可以用setRegistries()
        service.setProtocol(protocol); // 多个协议可以用setProtocols()
        service.setInterface(BackendService.class);
        service.setRef(backendService);
        // 暴露及注册服务
        service.export();
    }*/
}
