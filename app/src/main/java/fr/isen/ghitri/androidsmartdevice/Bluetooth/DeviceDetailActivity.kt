package fr.isen.ghitri.androidsmartdevice.Bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import java.util.*

class DeviceDetailActivity : ComponentActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private val btnMainUUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
    private val btn3UUID = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")
    private val mainBtnNotifUUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
    private val thirdBtnNotifUUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var led1On = mutableStateOf(false)
    private var led2On = mutableStateOf(false)
    private var led3On = mutableStateOf(false)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val address = intent.getStringExtra("device_address")
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (address == null || bluetoothAdapter == null) {
            Toast.makeText(this, "Erreur : Adresse invalide", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val device = bluetoothAdapter.getRemoteDevice(address)

        setContent {
            var connectionStatus by remember { mutableStateOf("🔄 Connexion à $address...") }
            var mainBtnClicks by remember { mutableStateOf("N/A") }
            var thirdBtnClicks by remember { mutableStateOf("N/A") }

            LaunchedEffect(true) {
                bluetoothGatt = device.connectGatt(this@DeviceDetailActivity, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                        super.onConnectionStateChange(gatt, status, newState)
                        connectionStatus = when (newState) {
                            BluetoothGatt.STATE_CONNECTED -> "✅ Connecté à l'appareil ($address)"
                            BluetoothGatt.STATE_DISCONNECTED -> "❌ Échec de la connexion"
                            else -> "⏳ Connexion en cours..."
                        }
                        gatt?.discoverServices()
                    }
                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        super.onServicesDiscovered(gatt, status)
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            enableNotificationForCharacteristic(gatt, mainBtnNotifUUID)
                            enableNotificationForCharacteristic(gatt, thirdBtnNotifUUID)
                        }
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        super.onCharacteristicRead(gatt, characteristic, status)
                        if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                            when (characteristic.uuid) {
                                btnMainUUID -> mainBtnClicks = characteristic.value.firstOrNull()?.toInt()?.toString() ?: "0"
                                btn3UUID -> thirdBtnClicks = characteristic.value.firstOrNull()?.toInt()?.toString() ?: "0"
                            }
                        }
                    }

                    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                        super.onCharacteristicChanged(gatt, characteristic)
                        runOnUiThread {
                            when (characteristic?.uuid) {
                                mainBtnNotifUUID -> {
                                    val count = characteristic?.value?.firstOrNull()?.toInt() ?: 0
                                    Toast.makeText(this@DeviceDetailActivity, "Bouton principal : $count clics", Toast.LENGTH_SHORT).show()
                                }
                                thirdBtnNotifUUID -> {
                                    val count = characteristic?.value?.firstOrNull()?.toInt() ?: 0
                                    Toast.makeText(this@DeviceDetailActivity, "Troisième bouton : $count clics", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                })
            }

            DeviceActionsUI(
                onToggleLed1 = {
                    if (led1On.value) {
                        // LED 1 est déjà allumée → on l'éteint
                        led1On.value = false
                        writeToLedChar(0x00) // valeur 0x00 pour éteindre toutes les LEDs
                    } else {
                        // On allume LED 1, on éteint les autres
                        led1On.value = true
                        led2On.value = false
                        led3On.value = false
                        writeToLedChar(0x01)
                    }
                },
                onToggleLed2 = {
                    if (led2On.value) {
                        led2On.value = false
                        writeToLedChar(0x00)
                    } else {
                        led1On.value = false
                        led2On.value = true
                        led3On.value = false
                        writeToLedChar(0x02)
                    }
                },
                onToggleLed3 = {
                    if (led3On.value) {
                        led3On.value = false
                        writeToLedChar(0x00)
                    } else {
                        led1On.value = false
                        led2On.value = false
                        led3On.value = true
                        writeToLedChar(0x03)
                    }
                },
                onReadMainButton = {
                    readValueFromChar(bluetoothGatt, btnMainUUID)
                },
                onReadThirdButton = {
                    readValueFromChar(bluetoothGatt, btn3UUID)
                },
                mainClickCount = mainBtnClicks,
                thirdClickCount = thirdBtnClicks,
                connectionStatus = connectionStatus,
                led1On = led1On.value,
                led2On = led2On.value,
                led3On = led3On.value
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt?.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeToLedChar(value: Int) {
        val services = bluetoothGatt?.services
        val ledCharacteristic = services?.getOrNull(2)?.characteristics?.getOrNull(0)
        ledCharacteristic?.let {
            it.value = byteArrayOf(value.toByte())
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun readValueFromChar(gatt: BluetoothGatt?, charUUID: UUID) {
        val characteristic = gatt?.services?.flatMap { it.characteristics }?.find { it.uuid == charUUID }
        if (characteristic != null) {
            gatt.readCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotificationForCharacteristic(gatt: BluetoothGatt?, uuid: UUID) {
        val characteristic = gatt?.services?.flatMap { it.characteristics }?.find { it.uuid == uuid }
        if (characteristic != null) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            }
        }
    }


    @Composable
    fun DeviceActionsUI(
        onToggleLed1: () -> Unit,
        onToggleLed2: () -> Unit,
        onToggleLed3: () -> Unit,
        onReadMainButton: () -> Unit,
        onReadThirdButton: () -> Unit,
        mainClickCount: String,
        thirdClickCount: String,
        connectionStatus: String,
        led1On: Boolean,
        led2On: Boolean,
        led3On: Boolean
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(connectionStatus, fontSize = 20.sp)
            Divider()
            Button(onClick = onToggleLed1) {
                Text(if (led1On) "Éteindre LED 1" else "Allumer LED 1")
            }
            Button(onClick = onToggleLed2) {
                Text(if (led2On) "Éteindre LED 2" else "Allumer LED 2")
            }
            Button(onClick = onToggleLed3) {
                Text(if (led3On) "Éteindre LED 3" else "Allumer LED 3")
            }
            Divider()
            Button(onClick = onReadMainButton) {
                Text("Lire clics bouton principal")
            }
            Text("👉 Clics bouton principal : $mainClickCount")
            Button(onClick = onReadThirdButton) {
                Text("Lire clics troisième bouton")
            }
            Text("👉 Clics bouton 3 : $thirdClickCount")
        }
    }
}