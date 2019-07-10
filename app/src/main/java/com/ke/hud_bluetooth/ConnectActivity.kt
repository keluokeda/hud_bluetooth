package com.ke.hud_bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.bletohud.DJBTManager
import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_connect.*
import java.util.*

class ConnectActivity : AppCompatActivity() {

    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var device: BluetoothDevice

    private var bluetoothSocket: BluetoothSocket? = null


    private val compositeDisposable = CompositeDisposable()


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            Logger.d("action = ${intent.action}")
        }
    }


    //    private val sendSubject = PublishSubject.create<ByteArray>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)


        val intentFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        }

        registerReceiver(receiver, intentFilter)

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

        if (device == null) {
            finish()
        }


        device_detail.text =
            "name = ${device.name}\naddress = ${device.address}\n配对状态 = ${getBondStateString(device.bondState)}"


        device_detail.setOnClickListener {

            connectDevice()

            //            bluetoothSocket.outputStream.write()
        }




        send_time.setOnClickListener {
            sendTime()
        }

        send_phone.setOnClickListener {
            sendPhone()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        compositeDisposable.dispose()
        unregisterReceiver(receiver)
    }

    private fun connectDevice() {

        Observable.just(1)
            .observeOn(Schedulers.io())
            .map {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)

                bluetoothSocket?.connect()

                return@map 1
            }

            .subscribe({
                Logger.d("连接设备成功")
            }, {
                Logger.d("连接设备失败")
                it.printStackTrace()
            })
            .addTo(compositeDisposable)

    }

    private val chatService = DJBTManager.getInstance()


    private fun sendTime() {

        chatService.sender.sendTime {

            //            sendSubject.onNext(it)
        }
    }


    private fun sendPhone() {
        chatService.sender.sendPhoneWithName(1, "123456", "大哥大") { byteArray ->
            //            sendSubject.onNext(it)


            Observable.just(1)
                .observeOn(Schedulers.io())
                .subscribe({
                    Logger.d("开始写入数据")

                    val outputStream = bluetoothSocket?.outputStream

                    if (outputStream == null) {
                        Logger.d("outputStream = null")
                    } else {
                        outputStream.write(byteArray)
                    }
                    Logger.d("写入数据完成")
                }, {
                    Logger.d("写入数据失败")
                    it.printStackTrace()
                })
        }
    }


    private fun getBondStateString(bondState: Int) = when (bondState) {
        BluetoothDevice.BOND_BONDED -> "已配对"
        BluetoothDevice.BOND_BONDING -> "配对中"
        BluetoothDevice.BOND_NONE -> "未配对"
        else -> "未知"
    }

}
