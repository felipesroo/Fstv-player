package com.fstv.player.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.fstv.player.databinding.ActivityPlayerBinding
import com.fstv.player.utils.ChannelItem
import com.fstv.player.utils.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private var channelsList: List<ChannelItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playlistUrl = intent.getStringExtra("PLAYLIST_URL")
        val customerName = intent.getStringExtra("CUSTOMER_NAME")

        binding.tvCustomerWelcome.text = "Bem-vindo, ${customerName ?: "Usuário"}"

        if (playlistUrl.isNull_or_Empty()) {
            Toast.makeText(this, "Nenhuma URL de playlist fornecida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initPlayer()
        loadPlaylist(playlistUrl)
    }

    private fun String?.isNull_or_Empty(): Boolean = this == null || this.isEmpty()

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
    }

    private fun loadPlaylist(url: String) {
        binding.progressBarPlayer.visibility = View.VISIBLE
        binding.tvLoadingStatus.text = "Carregando canais da lista M3U..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val m3uBody = response.body?.string() ?: ""
                    channelsList = M3uParser.parse(m3uBody)

                    withContext(Dispatchers.Main) {
                        binding.progressBarPlayer.visibility = View.GONE
                        binding.tvLoadingStatus.visibility = View.GONE
                        if (channelsList.isNotEmpty()) {
                            playChannel(channelsList[0])
                            Toast.makeText(this@PlayerActivity, "${channelsList.size} canais carregados!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@PlayerActivity, "Lista M3U vazia ou inválida", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.progressBarPlayer.visibility = View.GONE
                        binding.tvLoadingStatus.text = "Erro ao baixar lista M3U."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBarPlayer.visibility = View.GONE
                    binding.tvLoadingStatus.text = "Erro na rede ao baixar lista."
                }
            }
        }
    }

    private fun playChannel(channel: ChannelItem) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(Uri.parse(channel.streamUrl))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
