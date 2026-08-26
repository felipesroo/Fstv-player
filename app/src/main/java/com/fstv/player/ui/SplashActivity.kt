package com.fstv.player.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fstv.player.databinding.ActivitySplashBinding
import com.fstv.player.network.ApiClient
import com.fstv.player.utils.DeviceUtils
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    // Fallback padrão se nenhuma URL for definida no painel
    private val defaultFallbackUrl = "http://br22.lol/get.php?username=kppF9j&password=AbBf4V&type=m3u_plus&output=ts"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("FstvPlayerPrefs", Context.MODE_PRIVATE)
        val savedPlaylist = prefs.getString("SAVED_PLAYLIST_URL", null)
        val isActivated = prefs.getBoolean("IS_ACTIVATED", false)

        val mac = DeviceUtils.getMacAddress(this)
        val key = DeviceUtils.getDeviceKey(this)

        lifecycleScope.launch {
            try {
                // Registrar/verificar dispositivo no painel em segundo plano
                val response = ApiClient.apiService.checkDevice(mac, key)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status == "active") {
                        val activeUrl = if (!body.playlistUrl.isNullOrEmpty()) body.playlistUrl else defaultFallbackUrl
                        prefs.edit()
                            .putBoolean("IS_ACTIVATED", true)
                            .putString("SAVED_PLAYLIST_URL", activeUrl)
                            .putString("CUSTOMER_NAME", body.customerName)
                            .apply()

                        goToPlayer(activeUrl, body.customerName ?: "Cliente")
                        return@launch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Se já foi ativado anteriormente ou tem playlist salva
            if (isActivated && !savedPlaylist.isNullOrEmpty()) {
                val customer = prefs.getString("CUSTOMER_NAME", "Cliente") ?: "Cliente"
                goToPlayer(savedPlaylist, customer)
            } else if (!savedPlaylist.isNullOrEmpty()) {
                goToPlayer(savedPlaylist, "Cliente")
            } else {
                // Primeira vez sem ativação -> Abrir tela de MAC e Código de Ativação
                goToActivation()
            }
        }
    }

    private fun goToPlayer(url: String, customerName: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("PLAYLIST_URL", url)
            putExtra("CUSTOMER_NAME", customerName)
        }
        startActivity(intent)
        finish()
    }

    private fun goToActivation() {
        val intent = Intent(this, ActivationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
