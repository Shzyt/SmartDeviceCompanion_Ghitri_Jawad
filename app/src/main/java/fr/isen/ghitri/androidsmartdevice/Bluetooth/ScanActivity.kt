package fr.isen.ghitri.androidsmartdevice.Bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.isen.ghitri.androidsmartdevice.R

class ScanActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanResults = mutableListOf<ScanResult>()
    private var isScanning = false
    private val scanTimeout: Long = 10000
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var scanCallback: ScanCallback

    private val PERMISSIONS_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible sur cet appareil.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Veuillez activer le Bluetooth.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            finish()
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        val permissionsToRequest = getBluetoothPermissions()
        if (!hasPermissions(permissionsToRequest)) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            launchScanUI()
        }
    }

    private fun launchScanUI() {
        setContent {
            var scanning by remember { mutableStateOf(false) }
            scanResults = remember { mutableStateListOf<ScanResult>() }

            ScanScreen(
                isScanning = scanning,
                devices = scanResults,
                onToggleScan = {
                    if (scanning) {
                        stopScan()
                        scanning = false
                    } else {
                        startScan()
                        scanning = true
                    }
                }
            )
        }
    }


    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (isScanning) return

        scanResults.clear()
        isScanning = true

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    if (scanResults.none { it.device.address == result.device.address }) {
                        scanResults.add(result)
                    }
                }
            }
        }

        bluetoothLeScanner.startScan(scanCallback)

        handler.postDelayed({
            stopScan()
            Toast.makeText(this@ScanActivity, "Scan terminé (timeout)", Toast.LENGTH_SHORT).show()
        }, scanTimeout)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (!isScanning) return
        bluetoothLeScanner.stopScan(scanCallback)
        isScanning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isScanning) stopScan()
    }

    // ✅ Permission utilitaire dynamique
    fun getBluetoothPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
            else -> {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
        }
    }

    private fun hasPermissions(permissions: List<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                launchScanUI()
            } else {
                Toast.makeText(this, "Permissions refusées. L’application ne peut pas scanner les appareils BLE.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun ScanScreen(
        isScanning: Boolean,
        devices: List<ScanResult>,
        onToggleScan: () -> Unit
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Scan BLE", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = if (isScanning)
                    painterResource(id = R.drawable.ic_stop)
                else
                    painterResource(id = R.drawable.ic_play),
                contentDescription = "Scan BLE",
                modifier = Modifier
                    .size(100.dp)
                    .clickable { onToggleScan() }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if (isScanning) "🔎 Scan en cours..." else "⏸️ Scan arrêté")

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Appareils détectés :", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val intent = Intent(context, DeviceDetailActivity::class.java)
                                intent.putExtra("device_address", device.device.address)
                                context.startActivity(intent)
                            }
                    ) {
                        Text(
                            text = device.device.name ?: "Appareil inconnu",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
