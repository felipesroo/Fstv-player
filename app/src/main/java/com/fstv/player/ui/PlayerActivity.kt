package com.fstv.player.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class SectionType { LIVE_TV, MOVIES, SERIES }

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null

    // Listas por Seção (Canais, Filmes, Séries)
    private var liveTvList: List<ChannelItem> = emptyList()
    private var moviesList: List<ChannelItem> = emptyList()
    private var seriesList: List<ChannelItem> = emptyList()

    // Estado da tela atual
    private var currentSection: SectionType = SectionType.LIVE_TV
    private var currentSectionChannels: List<ChannelItem> = emptyList()
    private var currentCategoryChannels: List<ChannelItem> = emptyList()
    private var currentSelectedCategoryName: String = "Todos"

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private val channelInfoHandler = Handler(Looper.getMainLooper())
    private var activePlaylistUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playlistUrl = intent.getStringExtra("PLAYLIST_URL")
        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "Cliente"

        binding.tvCustomerName.text = "👋 Olá, $customerName"

        initPlayer()
        setupRecyclerViews()
        setupDashboardCards()
        setupSearch()

        activePlaylistUrl = if (playlistUrl.isNullOrEmpty()) {
            "http://br22.lol/get.php?username=kppF9j&password=AbBf4V&type=m3u_plus&output=ts"
        } else {
            playlistUrl
        }

        binding.btnRetryLoading.setOnClickListener {
            startFastPlaylistLoad(activePlaylistUrl)
        }

        startFastPlaylistLoad(activePlaylistUrl)
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        binding.playerView.useController = true
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter(emptyList()) { categoryInfo ->
            selectCategory(categoryInfo.name)
        }
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = categoryAdapter

        channelAdapter = ChannelAdapter(emptyList()) { channel, _ ->
            playChannel(channel)
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = channelAdapter

        binding.btnHome.setOnClickListener {
            showDashboard()
        }

        binding.btnBackToDashboard.setOnClickListener {
            showDashboard()
        }
    }

    private fun setupDashboardCards() {
        val cards = listOf(binding.cardLiveTv, binding.cardMovies, binding.cardSeries)
        for (card in cards) {
            card.setOnFocusChangeListener { v, hasFocus ->
                v.scaleX = if (hasFocus) 1.05f else 1.0f
                v.scaleY = if (hasFocus) 1.05f else 1.0f
                v.elevation = if (hasFocus) 12f else 0f
            }
        }

        binding.cardLiveTv.setOnClickListener { openSection(SectionType.LIVE_TV) }
        binding.cardMovies.setOnClickListener { openSection(SectionType.MOVIES) }
        binding.cardSeries.setOnClickListener { openSection(SectionType.SERIES) }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterCurrentCategory(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    /**
     * Carregamento Rápido com Cache Local + Processamento Direto de Categorias Nativas
     */
    private fun startFastPlaylistLoad(url: String) {
        val cacheFile = File(cacheDir, "cached_playlist.m3u")

        lifecycleScope.launch(Dispatchers.IO) {
            var loadedFromCache = false

            // 1. Carregar do Cache Local se existir (> 10KB)
            if (cacheFile.exists() && cacheFile.length() > 10240) {
                withContext(Dispatchers.Main) {
                    binding.tvLoadingStatus.text = "Iniciando lista em cache..."
                }
                try {
                    val stream = FileInputStream(cacheFile)
                    val channels = M3uParser.parseStream(stream)
                    stream.close()
                    if (channels.isNotEmpty()) {
                        loadedFromCache = true
                        processAndDisplayChannels(channels)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Exibir UI de Download se não carregou do cache
            if (!loadedFromCache) {
                withContext(Dispatchers.Main) {
                    binding.layoutLoading.visibility = View.VISIBLE
                    binding.progressBarPlayer.visibility = View.VISIBLE
                    binding.btnRetryLoading.visibility = View.GONE
                    binding.tvLoadingStatus.text = "Baixando lista de canais..."
                    binding.tvLoadingSub.text = "Aguarde enquanto a lista é obtida do servidor."
                }
            }

            // 3. Baixar versão atualizada do servidor
            val downloadOk = downloadAndCachePlaylist(url, cacheFile)

            if (!loadedFromCache && !downloadOk) {
                withContext(Dispatchers.Main) {
                    binding.progressBarPlayer.visibility = View.GONE
                    binding.tvLoadingStatus.text = "❌ Não foi possível baixar a lista"
                    binding.tvLoadingSub.text = "Verifique sua conexão de rede ou suporte."
                    binding.btnRetryLoading.visibility = View.VISIBLE
                    binding.btnRetryLoading.requestFocus()
                }
            }
        }
    }

    private suspend fun downloadAndCachePlaylist(url: String, cacheFile: File): Boolean {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/1.0.0")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body
                if (body != null) {
                    val tempFile = File(cacheDir, "temp_playlist.m3u")
                    val outputStream = FileOutputStream(tempFile)
                    body.byteStream().copyTo(outputStream)
                    outputStream.close()

                    if (tempFile.exists() && tempFile.length() > 1024) {
                        tempFile.copyTo(cacheFile, overwrite = true)
                        tempFile.delete()

                        val stream = FileInputStream(cacheFile)
                        val updatedChannels = M3uParser.parseStream(stream)
                        stream.close()

                        if (updatedChannels.isNotEmpty()) {
                            processAndDisplayChannels(updatedChannels)
                            return true
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun processAndDisplayChannels(allChannels: List<ChannelItem>) {
        val liveTv = mutableListOf<ChannelItem>()
        val movies = mutableListOf<ChannelItem>()
        val series = mutableListOf<ChannelItem>()

        val seriesRegex = Regex("(?i).*[ST]\\d{1,2}\\s*E\\d{1,3}.*")

        for (item in allChannels) {
            val catUpper = item.category.uppercase()
            val urlLower = item.streamUrl.lowercase()

            when {
                catUpper.contains("SÉR") || catUpper.contains("SERIE") || catUpper.contains("SÉRIE") || catUpper.contains("TEMPORADA") || catUpper.contains("ANIME") || catUpper.contains("NOVELA") || urlLower.contains("/series/") || seriesRegex.matches(item.name) -> {
                    series.add(item)
                }
                catUpper.contains("FILM") || catUpper.contains("MOVIE") || catUpper.contains("VOD") || catUpper.contains("CINEMA") || urlLower.contains("/movie/") -> {
                    movies.add(item)
                }
                else -> {
                    liveTv.add(item)
                }
            }
        }

        liveTvList = liveTv
        moviesList = movies
        seriesList = series

        withContext(Dispatchers.Main) {
            binding.layoutLoading.visibility = View.GONE
            binding.tvLiveTvCount.text = "${liveTvList.size} canais"
            binding.tvMoviesCount.text = "${moviesList.size} filmes"
            binding.tvSeriesCount.text = "${seriesList.size} itens"

            if (binding.layoutDashboard.visibility != View.VISIBLE && binding.layoutContent.visibility != View.VISIBLE) {
                showDashboard()
            }
        }
    }

    private fun showDashboard() {
        exoPlayer?.pause()

        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
        binding.layoutDashboard.visibility = View.VISIBLE
        binding.cardLiveTv.requestFocus()
    }

    private fun openSection(section: SectionType) {
        try {
            currentSection = section

            binding.layoutDashboard.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
            binding.tvSidebarHeader.text = "CATEGORIAS"

            when (section) {
                SectionType.LIVE_TV -> {
                    binding.tvSectionTitle.text = "📡 Canais ao Vivo (${liveTvList.size})"
                    currentSectionChannels = liveTvList
                }
                SectionType.MOVIES -> {
                    binding.tvSectionTitle.text = "🎬 Filmes (${moviesList.size})"
                    currentSectionChannels = moviesList
                }
                SectionType.SERIES -> {
                    binding.tvSectionTitle.text = "🎭 Séries (${seriesList.size})"
                    currentSectionChannels = seriesList
                }
            }

            // Construir lista de categorias da barra lateral esquerda a partir dos dados nativos
            val categoryMap = mutableMapOf<String, Int>()
            for (item in currentSectionChannels) {
                val catName = if (item.category.isEmpty()) "Geral" else item.category
                categoryMap[catName] = (categoryMap[catName] ?: 0) + 1
            }

            val categoryListInfo = mutableListOf<CategoryItemInfo>()
            categoryListInfo.add(CategoryItemInfo("Todos", currentSectionChannels.size, "📺"))

            for ((catName, count) in categoryMap.entries.sortedBy { it.key }) {
                val icon = getCategoryIcon(catName)
                categoryListInfo.add(CategoryItemInfo(catName, count, icon))
            }

            categoryAdapter.updateList(categoryListInfo)
            binding.rvCategories.requestFocus()

            selectCategory("Todos")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun selectCategory(categoryName: String) {
        try {
            currentSelectedCategoryName = categoryName

            binding.tvSidebarHeader.text = "CATEGORIAS"
            binding.tvCategoryHeader.text = "ITENS DA CATEGORIA: ${categoryName.uppercase()}"
            binding.etSearch.text?.clear()

            currentCategoryChannels = if (categoryName == "Todos") {
                currentSectionChannels
            } else {
                currentSectionChannels.filter { it.category.equals(categoryName, ignoreCase = true) }
            }

            channelAdapter.updateList(currentCategoryChannels)
            binding.rvChannels.scrollToPosition(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun filterCurrentCategory(query: String) {
        val filtered = if (query.isEmpty()) {
            currentCategoryChannels
        } else {
            currentCategoryChannels.filter { it.name.contains(query, ignoreCase = true) }
        }
        channelAdapter.updateList(filtered)
    }

    private fun getCategoryIcon(cat: String): String {
        val lower = cat.lowercase()
        return when {
            lower.contains("canal") || lower.contains("live") || lower.contains("tv") -> "📡"
            lower.contains("filme") || lower.contains("movie") || lower.contains("vod") -> "🎬"
            lower.contains("serie") || lower.contains("séri") || lower.contains("anime") -> "🎭"
            lower.contains("esport") || lower.contains("sport") || lower.contains("futebol") -> "⚽"
            lower.contains("kids") || lower.contains("desenho") || lower.contains("infant") -> "🧒"
            lower.contains("adult") || lower.contains("xxx") -> "🔞"
            lower.contains("notic") || lower.contains("news") -> "📰"
            else -> "📁"
        }
    }

    private fun playChannel(channel: ChannelItem) {
        val url = channel.streamUrl
        if (url.isEmpty()) return

        try {
            exoPlayer?.let { player ->
                val uri = Uri.parse(url.trim())
                val mediaItem = MediaItem.fromUri(uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
            }

            binding.tvCurrentChannelName.text = channel.name
            binding.tvCurrentCategory.text = channel.category
            binding.channelInfoOverlay.visibility = View.VISIBLE
            channelInfoHandler.removeCallbacksAndMessages(null)
            channelInfoHandler.postDelayed({
                binding.channelInfoOverlay.visibility = View.GONE
            }, 4000)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao tentar reproduzir o canal", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (binding.layoutContent.visibility == View.VISIBLE) {
                showDashboard()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        channelInfoHandler.removeCallbacksAndMessages(null)
        exoPlayer?.release()
        exoPlayer = null
    }
}
