package com.fstv.player.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fstv.player.databinding.ActivityActivationBinding
import com.fstv.player.network.ApiClient
import com.fstv.player.utils.DeviceUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivationBinding
    private var macAddress: String = ""
    private var deviceKey: String = ""
    private var pollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        macAddress = DeviceUtils.getMacAddress(this)
        deviceKey = DeviceUtils.getDeviceKey(this)

        binding.tvMacAddress.text = macAddress
        binding.tvDeviceKey.text = deviceKey

        binding.btnCheckActivation.setOnClickListener {
            checkActivationStatus(showToast = true)
        }

        registerDeviceOnServer()
    }

    override fun onResume() {
        super.onResume()
        startPollingActivation()
    }

    override fun onPause() {
        super.onPause()
        pollJob?.cancel()
    }

    private fun registerDeviceOnServer() {
        lifecycleScope.launch {
            try {
                binding.tvStatusMessage.text = "Conectando ao servidor..."
                val response = ApiClient.apiService.registerDevice(macAddress, deviceKey)
                if (response.isSuccessful) {
                    val body = response.body()
                    binding.tvStatusMessage.text = body?.message ?: "Aguardando ativação..."
                } else {
                    binding.tvStatusMessage.text = "Aguardando ativação no painel."
                }
            } catch (e: Exception) {
                binding.tvStatusMessage.text = "Verifique a conexão de rede da TV."
            }
        }
    }

    private fun startPollingActivation() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                checkActivationStatus(showToast = false)
                delay(5000) // Verifica a cada 5 segundos
            }
        }
    }

    private fun checkActivationStatus(showToast: Boolean) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.checkDevice(macAddress, deviceKey)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        when (body.status) {
                            "active" -> {
                                val playlistUrl = body.playlistUrl
                                if (playlistUrl.isNullOrEmpty()) {
                                    // Ativado mas sem playlist configurada ainda
                                    binding.tvStatusMessage.text = "✅ Ativado! Aguardando o administrador configurar a playlist no painel."
                                    binding.progressBar.visibility = View.GONE
                                    return@launch
                                }
                                pollJob?.cancel()
                                binding.tvStatusMessage.text = "✅ Dispositivo Ativado! Carregando canais..."
                                binding.progressBar.visibility = View.VISIBLE
                                
                                val intent = Intent(this@ActivationActivity, PlayerActivity::class.java).apply {
                                    putExtra("PLAYLIST_URL", playlistUrl)
                                    putExtra("CUSTOMER_NAME", body.customerName)
                                }
                                startActivity(intent)
                                finish()
                            }
                            "expired" -> {
                                binding.tvStatusMessage.text = "⚠️ Assinatura expirada. Contate o suporte para renovar."
                                binding.progressBar.visibility = View.GONE
                            }
                            "inactive" -> {
                                binding.tvStatusMessage.text = "❌ Dispositivo inativo no painel."
                                binding.progressBar.visibility = View.GONE
                            }
                            else -> {
                                binding.tvStatusMessage.text = "Aguardando ativação pelo MAC ou Código no painel web..."
                                binding.progressBar.visibility = View.GONE
                            }
                        }
                    }
                } else {
                    if (showToast) {
                        Toast.makeText(this@ActivationActivity, "Erro ao conectar ao servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (showToast) {
                    Toast.makeText(this@ActivationActivity, "Falha na conexão: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
