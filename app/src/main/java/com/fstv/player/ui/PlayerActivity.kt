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
import com.fstv.player.utils.SeriesShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class SectionType { LIVE_TV, MOVIES, SERIES }

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null

    // Listas filtradas por tipo de conteúdo
    private var liveTvList: List<ChannelItem> = emptyList()
    private var moviesList: List<ChannelItem> = emptyList()
    private var seriesList: List<ChannelItem> = emptyList()

    // Estado da tela atual
    private var currentSection: SectionType = SectionType.LIVE_TV
    private var currentSectionChannels: List<ChannelItem> = emptyList()
    private var currentCategoryChannels: List<ChannelItem> = emptyList()
    private var currentSelectedCategoryName: String = "Todos"

    // Modo de exibição de Séries (Se estamos vendo lista de séries ou episódios de uma série)
    private var isViewingSeriesEpisodes = false
    private var selectedShow: SeriesShow? = null

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

        if (playlistUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Nenhuma URL de playlist fornecida", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadPlaylist(playlistUrl)
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        binding.playerView.useController = true
    }

    private fun setupRecyclerViews() {
        // 1. Adapter da Sidebar Vertical de Categorias (Barra Lateral Esquerda)
        categoryAdapter = CategoryAdapter(emptyList()) { categoryInfo ->
            selectCategory(categoryInfo.name)
        }
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = categoryAdapter

        // 2. Adapter da Lista de Canais/Filmes/Séries
        channelAdapter = ChannelAdapter(emptyList()) { channel, _ ->
            onItemClicked(channel)
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = channelAdapter

        // 3. Botão Voltar ao Menu
        binding.btnBackToDashboard.setOnClickListener {
            if (isViewingSeriesEpisodes) {
                // Voltar da lista de episódios para a lista de séries da categoria
                selectCategory(currentSelectedCategoryName)
            } else {
                showDashboard()
            }
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

    private fun loadPlaylist(url: String) {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutDashboard.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
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
                    val allChannels = if (inputStream != null) M3uParser.parseStream(inputStream) else emptyList()

                    val liveTv = mutableListOf<ChannelItem>()
                    val movies = mutableListOf<ChannelItem>()
                    val series = mutableListOf<ChannelItem>()

                    val seriesRegex = Regex("(?i).*[ST]\\d{1,2}\\s*E\\d{1,3}.*")

                    for (item in allChannels) {
                        val cat = item.category.uppercase()
                        val name = item.name.uppercase()
                        val urlLower = item.streamUrl.lowercase()

                        when {
                            // 1. Regra de SÉRIES (Prioridade 1): Categoria com SÉR/SÉRIE/SÉRIES/TEMPORADA/ANIME ou formato S01E01
                            cat.contains("SÉR") || cat.contains("SERIE") || cat.contains("SÉRIE") || cat.contains("TEMPORADA") || cat.contains("ANIME") || urlLower.contains("/series/") || seriesRegex.matches(item.name) -> {
                                series.add(item)
                            }
                            // 2. Regra de FILMES (Prioridade 2): Categoria com FILME/FILMES/MOVIE/VOD/CINEMA
                            cat.contains("FILM") || cat.contains("MOVIE") || cat.contains("VOD") || cat.contains("CINEMA") || urlLower.contains("/movie/") -> {
                                movies.add(item)
                            }
                            // 3. Regra de CANAIS AO VIVO (Padrão)
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
                        binding.tvSeriesCount.text = "${seriesList.size} episódios de séries"

                        showDashboard()
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

    private fun showDashboard() {
        exoPlayer?.pause()
        isViewingSeriesEpisodes = false
        selectedShow = null

        binding.layoutLoading.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
        binding.layoutDashboard.visibility = View.VISIBLE
        binding.cardLiveTv.requestFocus()
    }

    private fun openSection(section: SectionType) {
        currentSection = section
        isViewingSeriesEpisodes = false
        selectedShow = null

        binding.layoutDashboard.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE

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

        // Construir lista de categorias da sidebar vertical
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
    }

    private fun selectCategory(categoryName: String) {
        currentSelectedCategoryName = categoryName
        isViewingSeriesEpisodes = false
        selectedShow = null

        binding.tvCategoryHeader.text = "ITENS DA CATEGORIA: ${categoryName.uppercase()}"
        binding.etSearch.text?.clear()

        val rawCategoryChannels = if (categoryName == "Todos") {
            currentSectionChannels
        } else {
            currentSectionChannels.filter { it.category.equals(categoryName, ignoreCase = true) }
        }

        if (currentSection == SectionType.SERIES) {
            // Se estiver na seção de Séries, agrupar episódios por título da Série!
            val shows = SeriesHelper.groupEpisodesByShow(rawCategoryChannels)
            // Transformar cada Série em um ChannelItem para exibição limpa
            currentCategoryChannels = shows.map { show ->
                ChannelItem(
                    name = "🎭 ${show.title} (${show.episodes.size} episódios)",
                    streamUrl = "SERIES_GROUP:${show.title}",
                    logoUrl = show.logoUrl,
                    category = show.category
                )
            }
        } else {
            currentCategoryChannels = rawCategoryChannels
        }

        channelAdapter.updateList(currentCategoryChannels)
        binding.rvChannels.scrollToPosition(0)

        if (currentCategoryChannels.isNotEmpty() && currentSection == SectionType.LIVE_TV) {
            playChannel(currentCategoryChannels[0])
        }
    }

    private fun onItemClicked(item: ChannelItem) {
        if (item.streamUrl.startsWith("SERIES_GROUP:")) {
            // O usuário clicou em uma Série -> Exibir os episódios dessa série!
            val showTitle = item.streamUrl.removePrefix("SERIES_GROUP:")
            val episodes = currentSectionChannels.filter {
                SeriesHelper.extractShowTitle(it.name).equals(showTitle, ignoreCase = true)
            }

            isViewingSeriesEpisodes = true
            binding.tvCategoryHeader.text = "EPISÓDIOS: ${showTitle.uppercase()}"

            channelAdapter.updateList(episodes)
            binding.rvChannels.scrollToPosition(0)
            Toast.makeText(this, "📺 ${episodes.size} episódios de $showTitle", Toast.LENGTH_SHORT).show()
        } else {
            playChannel(item)
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
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(Uri.parse(channel.streamUrl))
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isViewingSeriesEpisodes) {
                selectCategory(currentSelectedCategoryName)
                return true
            } else if (binding.layoutContent.visibility == View.VISIBLE) {
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
