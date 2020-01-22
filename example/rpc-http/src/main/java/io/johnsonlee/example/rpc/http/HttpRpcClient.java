package io.johnsonlee.example.rpc.http;

import com.google.auto.service.AutoService;

import io.johnsonlee.example.rpc.RpcClient;

@AutoService(RpcClient.class)
public class HttpRpcClient implements RpcClient {

    @Override
    public String getProtocol() {
        return "http";
    }

}
