package fr.isen.ghitri.androidsmartdevice.Bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

class DeviceDetailActivity : ComponentActivity() {
    private var bluetoothGatt: BluetoothGatt? = null

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
            var connectionStatus by remember { mutableStateOf("Connexion à $address...") }

            LaunchedEffect(true) {
                bluetoothGatt = device.connectGatt(this@DeviceDetailActivity, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                        super.onConnectionStateChange(gatt, status, newState)
                        connectionStatus = when (newState) {
                            BluetoothGatt.STATE_CONNECTED -> "✅ Connecté à $address"
                            BluetoothGatt.STATE_DISCONNECTED -> "❌ Déconnecté"
                            else -> "🔄 Connexion en cours..."
                        }
                    }
                })
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = connectionStatus)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
    }
}
