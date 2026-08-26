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
import com.fstv.player.R
import com.fstv.player.databinding.ActivityPlayerBinding
import com.fstv.player.utils.ChannelItem
import com.fstv.player.utils.M3uParser
import com.fstv.player.utils.SeriesHelper
import com.fstv.player.utils.SeriesShowWithSeasons
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

    // Listas de conteúdo
    private var liveTvList: List<ChannelItem> = emptyList()
    private var moviesList: List<ChannelItem> = emptyList()
    private var seriesList: List<ChannelItem> = emptyList()

    // Séries com Temporadas
    private var preGroupedSeriesShows: List<SeriesShowWithSeasons> = emptyList()
    private var preGroupedSeriesChannels: List<ChannelItem> = emptyList()

    // Estado da tela atual
    private var currentSection: SectionType = SectionType.LIVE_TV
    private var currentSectionChannels: List<ChannelItem> = emptyList()
    private var currentCategoryChannels: List<ChannelItem> = emptyList()
    private var currentSelectedCategoryName: String = "Todos"

    // Navegação em Séries (Série -> Temporada -> Episódios)
    private var selectedShow: SeriesShowWithSeasons? = null
    private var selectedSeasonNumber: Int = 1

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private val channelInfoHandler = Handler(Looper.getMainLooper())

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

        val finalUrl = if (playlistUrl.isNullOrEmpty()) {
            "http://br22.lol/get.php?username=kppF9j&password=AbBf4V&type=m3u_plus&output=ts"
        } else {
            playlistUrl
        }

        startFastPlaylistLoad(finalUrl)
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        binding.playerView.useController = true
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter(emptyList()) { categoryInfo ->
            if (selectedShow != null) {
                // Seleção de Temporada dentro de uma Série
                val tempNum = categoryInfo.name.removePrefix("Temporada ").toIntOrNull() ?: 1
                selectSeason(tempNum)
            } else {
                // Seleção de Categoria normal
                selectCategory(categoryInfo.name)
            }
        }
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = categoryAdapter

        channelAdapter = ChannelAdapter(emptyList()) { channel, _ ->
            onItemClicked(channel)
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = channelAdapter

        // Botão 🏠 INÍCIO (Volta direto ao Dashboard)
        binding.btnHome.setOnClickListener {
            showDashboard()
        }

        // Botão ← Voltar (Volta 1 nível)
        binding.btnBackToDashboard.setOnClickListener {
            handleBackStep()
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

    private fun startFastPlaylistLoad(url: String) {
        val cacheFile = File(cacheDir, "cached_playlist.m3u")

        lifecycleScope.launch(Dispatchers.IO) {
            if (cacheFile.exists() && cacheFile.length() > 0) {
                withContext(Dispatchers.Main) {
                    binding.tvLoadingStatus.text = "Iniciando lista em cache..."
                }
                try {
                    val channels = M3uParser.parseStream(FileInputStream(cacheFile))
                    if (channels.isNotEmpty()) {
                        processAndDisplayChannels(channels, isFromCache = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.layoutLoading.visibility = View.VISIBLE
                    binding.tvLoadingStatus.text = "Baixando lista M3U pela primeira vez..."
                }
            }

            downloadAndCachePlaylist(url, cacheFile)
        }
    }

    private suspend fun downloadAndCachePlaylist(url: String, cacheFile: File) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/1.0.0")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val inputStream = response.body?.byteStream()
                if (inputStream != null) {
                    val tempFile = File(cacheDir, "temp_playlist.m3u")
                    val outputStream = FileOutputStream(tempFile)
                    inputStream.copyTo(outputStream)
                    outputStream.close()
                    inputStream.close()

                    if (tempFile.exists() && tempFile.length() > 0) {
                        tempFile.copyTo(cacheFile, overwrite = true)
                        tempFile.delete()

                        val updatedChannels = M3uParser.parseStream(FileInputStream(cacheFile))
                        if (updatedChannels.isNotEmpty()) {
                            processAndDisplayChannels(updatedChannels, isFromCache = false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun processAndDisplayChannels(allChannels: List<ChannelItem>, isFromCache: Boolean) {
        val liveTv = mutableListOf<ChannelItem>()
        val movies = mutableListOf<ChannelItem>()
        val series = mutableListOf<ChannelItem>()

        val seriesRegex = Regex("(?i).*[ST]\\d{1,2}\\s*E\\d{1,3}.*")

        for (item in allChannels) {
            val cat = item.category.uppercase()
            val urlLower = item.streamUrl.lowercase()

            when {
                cat.contains("SÉR") || cat.contains("SERIE") || cat.contains("SÉRIE") || cat.contains("TEMPORADA") || cat.contains("ANIME") || urlLower.contains("/series/") || seriesRegex.matches(item.name) -> {
                    series.add(item)
                }
                cat.contains("FILM") || cat.contains("MOVIE") || cat.contains("VOD") || cat.contains("CINEMA") || urlLower.contains("/movie/") -> {
                    movies.add(item)
                }
                else -> {
                    liveTv.add(item)
                }
            }
        }

        // Pré-agrupar Séries por Título e Temporadas em background
        val groupedShows = SeriesHelper.groupEpisodesByShowAndSeason(series)
        val groupedChannels = groupedShows.map { show ->
            val totalSeasons = show.seasonsMap.size
            val totalEpisodes = show.seasonsMap.values.sumOf { it.size }
            ChannelItem(
                name = "🎭 ${show.title} ($totalSeasons Temp. | $totalEpisodes Ep.)",
                streamUrl = "SERIES_SHOW:${show.title}",
                logoUrl = show.logoUrl,
                category = show.category
            )
        }

        liveTvList = liveTv
        moviesList = movies
        seriesList = series
        preGroupedSeriesShows = groupedShows
        preGroupedSeriesChannels = groupedChannels

        withContext(Dispatchers.Main) {
            binding.layoutLoading.visibility = View.GONE
            binding.tvLiveTvCount.text = "${liveTvList.size} canais"
            binding.tvMoviesCount.text = "${moviesList.size} filmes"
            binding.tvSeriesCount.text = "${groupedShows.size} séries"

            if (binding.layoutDashboard.visibility != View.VISIBLE && binding.layoutContent.visibility != View.VISIBLE) {
                showDashboard()
            }
        }
    }

    private fun showDashboard() {
        exoPlayer?.pause()
        selectedShow = null
        isViewingSeriesEpisodes = false

        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
        binding.layoutDashboard.visibility = View.VISIBLE
        binding.cardLiveTv.requestFocus()
    }

    private fun openSection(section: SectionType) {
        try {
            currentSection = section
            selectedShow = null
            isViewingSeriesEpisodes = false

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
                    binding.tvSectionTitle.text = "🎭 Séries (${preGroupedSeriesShows.size} séries)"
                    currentSectionChannels = seriesList
                }
            }

            val categoryMap = mutableMapOf<String, Int>()
            if (section == SectionType.SERIES) {
                for (show in preGroupedSeriesShows) {
                    val catName = if (show.category.isEmpty()) "Geral" else show.category
                    categoryMap[catName] = (categoryMap[catName] ?: 0) + 1
                }
            } else {
                for (item in currentSectionChannels) {
                    val catName = if (item.category.isEmpty()) "Geral" else item.category
                    categoryMap[catName] = (categoryMap[catName] ?: 0) + 1
                }
            }

            val categoryListInfo = mutableListOf<CategoryItemInfo>()
            val totalCount = if (section == SectionType.SERIES) preGroupedSeriesShows.size else currentSectionChannels.size
            categoryListInfo.add(CategoryItemInfo("Todos", totalCount, "📺"))

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
            selectedShow = null
            isViewingSeriesEpisodes = false

            binding.tvSidebarHeader.text = "CATEGORIAS"
            binding.tvCategoryHeader.text = "SÉRIES DA CATEGORIA: ${categoryName.uppercase()}"
            binding.etSearch.text?.clear()

            if (currentSection == SectionType.SERIES) {
                currentCategoryChannels = if (categoryName == "Todos") {
                    preGroupedSeriesChannels
                } else {
                    preGroupedSeriesChannels.filter { it.category.equals(categoryName, ignoreCase = true) }
                }
            } else {
                currentCategoryChannels = if (categoryName == "Todos") {
                    currentSectionChannels
                } else {
                    currentSectionChannels.filter { it.category.equals(categoryName, ignoreCase = true) }
                }
            }

            channelAdapter.updateList(currentCategoryChannels)
            binding.rvChannels.scrollToPosition(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSeriesShow(show: SeriesShowWithSeasons) {
        selectedShow = show
        binding.tvSidebarHeader.text = "TEMPORADAS DE: ${show.title.uppercase()}"
        binding.tvCategoryHeader.text = "EPISÓDIOS DA SÉRIE"

        // Preencher a Sidebar de Categorias com as Temporadas da Série!
        val seasonCategoryInfo = mutableListOf<CategoryItemInfo>()
        for ((seasonNum, episodes) in show.seasonsMap) {
            seasonCategoryInfo.add(
                CategoryItemInfo(
                    name = "Temporada $seasonNum",
                    count = episodes.size,
                    icon = "📅"
                )
            )
        }

        categoryAdapter.updateList(seasonCategoryInfo)
        binding.rvCategories.requestFocus()

        // Selecionar Temporada 1 (ou primeira disponível) por padrão
        val firstSeasonNum = show.seasonsMap.keys.firstOrNull() ?: 1
        selectSeason(firstSeasonNum)
    }

    private fun selectSeason(seasonNumber: Int) {
        selectedSeasonNumber = seasonNumber
        val show = selectedShow ?: return
        val epList = show.seasonsMap[seasonNumber] ?: emptyList()

        binding.tvCategoryHeader.text = "EPISÓDIOS: ${show.title.uppercase()} (TEMPORADA $seasonNumber)"

        val channelItems = epList.map { ep ->
            ChannelItem(
                name = ep.displayName,
                streamUrl = ep.item.streamUrl,
                logoUrl = ep.item.logoUrl,
                category = show.title
            )
        }

        currentCategoryChannels = channelItems
        channelAdapter.updateList(currentCategoryChannels)
        binding.rvChannels.scrollToPosition(0)
    }

    private fun onItemClicked(item: ChannelItem) {
        try {
            if (item.streamUrl.startsWith("SERIES_SHOW:")) {
                val showTitle = item.streamUrl.removePrefix("SERIES_SHOW:")
                val show = preGroupedSeriesShows.find { it.title.equals(showTitle, ignoreCase = true) }
                if (show != null) {
                    openSeriesShow(show)
                }
            } else {
                playChannel(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleBackStep() {
        if (selectedShow != null) {
            // Se estiver vendo uma Série/Temporada -> Voltar para Lista de Séries da Categoria
            selectCategory(currentSelectedCategoryName)
            // Atualizar lista de categorias da barra lateral
            openSection(SectionType.SERIES)
        } else if (binding.layoutContent.visibility == View.VISIBLE) {
            // Se estiver na Lista de Conteúdo -> Voltar para o Dashboard Inicial
            showDashboard()
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
        if (url.isEmpty() || url.startsWith("SERIES_SHOW:")) return

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
                handleBackStep()
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
