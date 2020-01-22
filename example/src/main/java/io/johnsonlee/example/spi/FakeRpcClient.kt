package io.johnsonlee.example.spi

import com.google.auto.service.AutoService
import io.johnsonlee.example.rpc.RpcClient

@AutoService(RpcClient::class)
class FakeRpcClient : RpcClient {

    override fun getProtocol() = "fake"

}