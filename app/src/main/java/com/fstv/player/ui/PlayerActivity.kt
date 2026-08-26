package com.fstv.player.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
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
    private var allChannels: List<ChannelItem> = emptyList()
    private var filteredChannels: List<ChannelItem> = emptyList()
    private var categories: List<String> = emptyList()
    private var currentCategory: String = "Todos"
    private lateinit var channelAdapter: ChannelAdapter
    private val channelInfoHandler = Handler(Looper.getMainLooper())
    private var sidebarVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playlistUrl = intent.getStringExtra("PLAYLIST_URL")
        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "Cliente"

        binding.tvCustomerWelcome.text = customerName

        initPlayer()
        setupRecyclerView()
        setupSearch()
        setupToggleButton()

        if (playlistUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Nenhuma URL de playlist fornecida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadPlaylist(playlistUrl)
    }

    private fun setupToggleButton() {
        binding.btnToggleSidebar.setOnClickListener { toggleSidebar() }
    }

    private fun toggleSidebar() {
        sidebarVisible = !sidebarVisible
        binding.sidebar.visibility = if (sidebarVisible) View.VISIBLE else View.GONE
        if (sidebarVisible) {
            binding.rvChannels.requestFocus()
        } else {
            binding.playerView.requestFocus()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!sidebarVisible) {
                    toggleSidebar()
                    true
                } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_BACK -> {
                if (sidebarVisible) {
                    toggleSidebar()
                    true
                } else super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        binding.playerView.useController = true
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(emptyList()) { channel ->
            playChannel(channel)
            // Fechar sidebar ao selecionar canal
            sidebarVisible = false
            binding.sidebar.visibility = View.GONE
            binding.playerView.requestFocus()
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = channelAdapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterChannels(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterChannels(query: String) {
        val base = if (currentCategory == "Todos") allChannels
                   else allChannels.filter { it.category.equals(currentCategory, ignoreCase = true) }

        filteredChannels = if (query.isEmpty()) base
                           else base.filter { it.name.contains(query, ignoreCase = true) }

        channelAdapter.updateList(filteredChannels)
    }

    private fun buildCategoryTabs() {
        binding.tabsContainer.removeAllViews()

        val allCats = listOf("Todos") + categories

        // Identificar categorias conhecidas
        val mainCats = mutableListOf<String>()
        val outros = mutableListOf<String>()

        for (cat in allCats) {
            val lower = cat.lowercase()
            val isMain = lower == "todos" ||
                lower.contains("canal") ||
                lower.contains("live") ||
                lower.contains("ao vivo") ||
                lower.contains("filme") ||
                lower.contains("movie") ||
                lower.contains("serie") ||
                lower.contains("séri") ||
                lower.contains("esport") ||
                lower.contains("adult") ||
                lower.contains("kids") ||
                lower.contains("notic") ||
                lower.contains("sport")
            if (isMain) mainCats.add(cat) else outros.add(cat)
        }

        val orderedCats = mainCats + outros

        for (cat in orderedCats) {
            val tab = TextView(this).apply {
                text = categoryLabel(cat)
                textSize = 13f
                setPadding(20, 12, 20, 12)
                isFocusable = true
                isClickable = true
                setTextColor(if (cat == currentCategory) 0xFF6366F1.toInt() else 0xCCFFFFFF.toInt())
                setBackgroundColor(if (cat == currentCategory) 0x1A6366F1 else 0x00000000)

                setOnClickListener {
                    currentCategory = cat
                    binding.etSearch.text?.clear()
                    filterChannels("")
                    buildCategoryTabs()
                }
            }
            binding.tabsContainer.addView(tab)
        }
    }

    private fun categoryLabel(cat: String): String {
        val lower = cat.lowercase()
        return when {
            lower == "todos" -> "📺 Todos"
            lower.contains("canal") || lower.contains("live") || lower.contains("ao vivo") -> "📡 ${cat}"
            lower.contains("filme") || lower.contains("movie") -> "🎬 ${cat}"
            lower.contains("serie") || lower.contains("séri") -> "🎭 ${cat}"
            lower.contains("esport") || lower.contains("sport") -> "⚽ ${cat}"
            lower.contains("kids") || lower.contains("infant") -> "🧒 ${cat}"
            lower.contains("adult") || lower.contains("xxx") -> "🔞 ${cat}"
            else -> cat
        }
    }

    private fun loadPlaylist(url: String) {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.tvLoadingStatus.text = "Carregando lista M3U..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val m3uBody = response.body?.string() ?: ""
                    allChannels = M3uParser.parse(m3uBody)
                    filteredChannels = allChannels

                    // Extrair categorias únicas
                    categories = allChannels.map { it.category }.distinct().sorted()

                    withContext(Dispatchers.Main) {
                        binding.loadingOverlay.visibility = View.GONE
                        if (allChannels.isNotEmpty()) {
                            channelAdapter.updateList(allChannels)
                            buildCategoryTabs()

                            // Abrir sidebar automaticamente
                            sidebarVisible = true
                            binding.sidebar.visibility = View.VISIBLE

                            // Reproduzir primeiro canal automaticamente
                            playChannel(allChannels[0])

                            Toast.makeText(
                                this@PlayerActivity,
                                "✅ ${allChannels.size} canais em ${categories.size} categorias",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            binding.tvLoadingStatus.text = "Lista vazia ou inválida."
                            binding.loadingOverlay.visibility = View.VISIBLE
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.tvLoadingStatus.text = "Erro ao baixar lista (${response.code})."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvLoadingStatus.text = "Erro de rede: ${e.message}"
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

        // Mostrar overlay com nome do canal
        binding.tvCurrentChannelName.text = channel.name
        binding.tvCurrentCategory.text = channel.category
        binding.channelInfoOverlay.visibility = View.VISIBLE
        channelInfoHandler.removeCallbacksAndMessages(null)
        channelInfoHandler.postDelayed({
            binding.channelInfoOverlay.visibility = View.GONE
        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        channelInfoHandler.removeCallbacksAndMessages(null)
        exoPlayer?.release()
        exoPlayer = null
    }
}
