package com.example.usb_host_api;

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

open class USBManagerHelper(
    private val context: Context? = null,
    private val usbManager: UsbManager? = null
) {
    var usbDevice: UsbDevice? = null
        private set

    /**
     * Detects USB devices connected to the Android device.
     */
    open fun detectUsbDevice(
        onDeviceDetected: (UsbDevice) -> Unit,
        onNoDeviceDetected: () -> Unit,
        onPermissionRequired: (UsbDevice) -> Unit
    ) {
        val deviceList = usbManager?.deviceList
        Log.d("USB_DETECTION", "Device List: $deviceList")
        if (deviceList?.isNotEmpty() == true) {
            for ((_, device) in deviceList) {
                Log.d("USB_DEBUG", "Device found: ${device.deviceName}")
            }
        } else {
            Log.d("USB_DEBUG", "No devices found")
        }

        if (deviceList?.isNotEmpty() == true) {
            usbDevice = deviceList.values.first()
            usbDevice?.let {
                if (usbManager.hasPermission(it)) {
                    onDeviceDetected(it)
                } else {
                    onPermissionRequired(it)
                }
            }
        } else {
            onNoDeviceDetected()
        }
    }

    /**
     * Requests permission for the specified USB device.
     */
    fun requestUsbPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("com.example.USB_PERMISSION"),
            PendingIntent.FLAG_IMMUTABLE
        )
        usbManager?.requestPermission(device, permissionIntent)
    }

    /**
     * Synchronizes data with the connected USB device.
     */
    open fun syncData(
        device: UsbDevice,
        onDataTransferred: (String) -> Unit,
        onTransferFailed: () -> Unit
    ) {
        val connection = usbManager?.openDevice(device)
        val endpoint = device.getInterface(0).getEndpoint(0) // Assuming endpoint 0

        connection?.apply {
            val buffer = ByteArray(64) // Adjust size as needed
            val result = bulkTransfer(endpoint, buffer, buffer.size, 5000)
            if (result >= 0) {
                onDataTransferred(buffer.decodeToString())
            } else {
                onTransferFailed()
            }
            close()
        }
    }
}
