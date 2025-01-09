package com.example.usb_host_api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usb_host_api.ui.theme.USBHostAPITheme
import me.jahnen.libaums.core.UsbMassStorageDevice

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.usb_host_api.USB_PERMISSION"
    }

    private lateinit var usbManagerHelper: USBManagerHelper
    private var statusMessage by mutableStateOf("No USB Device Connected")
    private var connectedDevice by mutableStateOf<UsbDevice?>(null)
    private var isSyncButtonVisible by mutableStateOf(false)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> handleDeviceAttached()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> handleDeviceDetached()
                ACTION_USB_PERMISSION -> handlePermissionResult(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val devices = UsbMassStorageDevice.getMassStorageDevices(this /* Context or Activity */)

        for (device in devices) {

            // before interacting with a device you need to call init()!
            device.init()

            // Only uses the first partition on the device
            val currentFs = device.partitions[0].fileSystem
            Log.d("TAG", "Capacity: " + currentFs.capacity)
            Log.d("TAG", "Occupied Space: " + currentFs.occupiedSpace)
            Log.d("TAG", "Free Space: " + currentFs.freeSpace)
            Log.d("TAG", "Chunk size: " + currentFs.chunkSize)
        }

        val hasUsbHostFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        Log.d("USB_DEBUG", "USB Host Mode Supported: $hasUsbHostFeature")


        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        usbManagerHelper = USBManagerHelper(this, usbManager)

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        registerReceiver(usbReceiver, filter)

        setContent {
            USBHostAPITheme {
                USBHostApp(
                    usbManagerHelper = usbManagerHelper,
                    statusMessage = statusMessage,
                    isSyncButtonVisible = isSyncButtonVisible,
                    connectedDevice = connectedDevice
                )
            }
        }
    }

    private fun handleDeviceAttached() {
        usbManagerHelper.detectUsbDevice(
            onDeviceDetected = { device ->
                connectedDevice = device
                statusMessage = "USB Device Connected: ${device.deviceName}"
                isSyncButtonVisible = true
            },
            onNoDeviceDetected = {
                statusMessage = "No USB Device Connected"
                isSyncButtonVisible = false
            },
            onPermissionRequired = { device ->
                statusMessage = "USB Permission Required"
                usbManagerHelper.requestUsbPermission(device)
            }
        )
    }

    private fun handleDeviceDetached() {
        statusMessage = "USB Device Disconnected"
        connectedDevice = null
        isSyncButtonVisible = false
    }

    private fun handlePermissionResult(intent: Intent) {
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            device?.let {
                statusMessage = "USB Permission Granted"
                connectedDevice = it
                isSyncButtonVisible = true
            }
        } else {
            statusMessage = "USB Permission Denied"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }
}

@Composable
fun USBHostApp(
    usbManagerHelper: USBManagerHelper,
    statusMessage: String,
    isSyncButtonVisible: Boolean,
    connectedDevice: UsbDevice?
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = statusMessage)

                if (isSyncButtonVisible) {
                    Button(
                        onClick = {
                            connectedDevice?.let { device ->
                                usbManagerHelper.syncData(
                                    device,
                                    onDataTransferred = { data ->
//                                        statusMessage = "Data Transferred: $data"
                                    },
                                    onTransferFailed = {
//                                        statusMessage = "Data Transfer Failed"
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Sync Data")
                    }
                }

                Button(
                    onClick = {
                        usbManagerHelper.detectUsbDevice(
                            onDeviceDetected = { device ->
//                                connectedDevice = device
//                                statusMessage = "USB Device Connected: ${device.deviceName}"
//                                isSyncButtonVisible = true
                            },
                            onNoDeviceDetected = {
//                                statusMessage = "No USB Device Connected"
//                                isSyncButtonVisible = false
                            },
                            onPermissionRequired = { device ->
//                                statusMessage = "USB Permission Required"
                                usbManagerHelper.requestUsbPermission(device)
                            }
                        )
                    }
                ) {
                    Text("Detect USB Device")
                }
            }
        }
    )
}
