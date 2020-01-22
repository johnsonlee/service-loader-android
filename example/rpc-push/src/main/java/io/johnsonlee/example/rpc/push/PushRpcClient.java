package io.johnsonlee.example.rpc.push;

import com.google.auto.service.AutoService;

import io.johnsonlee.example.rpc.RpcClient;

@AutoService(RpcClient.class)
public class PushRpcClient implements RpcClient {

    @Override
    public String getProtocol() {
        return "push";
    }

}
