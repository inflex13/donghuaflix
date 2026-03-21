package com.donghuaflix.data.repository

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import com.donghuaflix.data.local.dao.ShowDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePreloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val showDao: ShowDao,
) {
    suspend fun preloadAllPosters() = withContext(Dispatchers.IO) {
        val shows = showDao.getRecentShows(40)
        shows.forEach { show ->
            val url = show.posterUrl ?: return@forEach
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(300, 450)
                .build()
            imageLoader.execute(request)
        }
    }
}
