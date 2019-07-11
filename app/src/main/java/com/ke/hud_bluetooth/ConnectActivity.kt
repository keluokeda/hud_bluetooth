package com.ke.hud_bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
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


            when (intent.action) {

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)

                    Logger.d("配对状态发生变化 $device ${getBondStateString(bondState)}")
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    device_state.setImageResource(R.drawable.ic_bluetooth_connected_green_500_24dp)
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    device_state.setImageResource(R.drawable.ic_bluetooth_disabled_red_500_24dp)
                }
            }
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
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        registerReceiver(receiver, intentFilter)

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

        if (device == null) {
            finish()
        }


        device_detail.text =
            "name = ${device.name}\naddress = ${device.address}\n配对状态 = ${getBondStateString(device.bondState)}"




        initListener()
    }

    private fun initListener() {
        create_bond.setOnClickListener {
            bondDevice()
        }

        connect_device.setOnClickListener {
            connectDevice()
        }


        send_time.setOnClickListener {
            sendTime()
        }

        send_phone.setOnClickListener {
            sendPhone()
        }

        send_navigation.setOnClickListener {
            sendNavigationInfo()
        }

        send_image.setOnClickListener {
            sendImage()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        compositeDisposable.dispose()
        unregisterReceiver(receiver)
//        bluetoothSocket?.close()
    }

    /**
     * 配对设备
     */
    private fun bondDevice() {
        val result = device.createBond()

        Logger.d("创建配对结果 $result")
    }

    /**
     * 连接设备
     */
    private fun connectDevice() {

        val startTime = System.currentTimeMillis()

        Logger.d("开始连接设备")

        Observable.just(1)
            .observeOn(Schedulers.io())
            .map {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)

                bluetoothSocket?.connect()

                return@map 1
            }

            .subscribe({
                Logger.d("连接设备成功 ${System.currentTimeMillis() - startTime}")
            }, {
                Logger.d("连接设备失败 ${System.currentTimeMillis() - startTime}")
                it.printStackTrace()
            })
            .addTo(compositeDisposable)

    }

    private val chatService = DJBTManager.getInstance()


    private fun sendTime() {

        chatService.sender.sendTime {

            writeByteArray(it)
            //            sendSubject.onNext(it)
        }
    }


    private fun sendPhone() {
        chatService.sender.sendPhoneWithName(1, "123456", "大哥大") { byteArray ->
            //            sendSubject.onNext(it)


            writeByteArray(byteArray)
        }
    }

    private fun sendNavigationInfo() {

        val distance = Random().nextInt(2000)
        chatService.sender.sendNavigationInformationWithDirection(3, distance, "当前道路", "下一个道路", 100, 1000, 66) {
            writeByteArray(it)
        }
    }

    private fun sendImage() {
        val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.show_cross_09_12_43)

        chatService.sender.sendImg(bitmap) {
            writeByteArray(it)
        }
    }


    private fun writeByteArray(byteArray: ByteArray) {
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
                Logger.d("发送数据失败")
                it.printStackTrace()
            })
            .addTo(compositeDisposable)
    }

    private fun getBondStateString(bondState: Int) = when (bondState) {
        BluetoothDevice.BOND_BONDED -> "已配对"
        BluetoothDevice.BOND_BONDING -> "配对中"
        BluetoothDevice.BOND_NONE -> "未配对"
        else -> "未知"
    }

}
