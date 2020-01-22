package io.johnsonlee.example.spi

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.johnsonlee.example.rpc.RpcClient
import java.util.*

class MainActivity : AppCompatActivity() {

    private val clients = ServiceLoader.load(RpcClient::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val protocls = clients.joinToString("\n") {
            it.protocol + " => " + it.javaClass.name
        }
        setContentView(TextView(this).apply {
            text = protocls
        })
    }

}