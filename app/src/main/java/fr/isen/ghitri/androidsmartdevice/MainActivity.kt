package fr.isen.ghitri.androidsmartdevice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isen.ghitri.androidsmartdevice.Bluetooth.ScanActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen {
                startActivity(Intent(this, ScanActivity::class.java))
            }
        }
    }
}

@Composable
fun MainScreen(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_ble), // Ajoute ton icône ici
            contentDescription = "App Icon",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Android Smart Device", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Découvrez et connectez-vous aux appareils BLE à proximité.")
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onStartScan) {
            Text("Démarrer le Scan")
        }
    }
}