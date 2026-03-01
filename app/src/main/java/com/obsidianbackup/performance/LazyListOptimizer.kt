// performance/LazyListOptimizer.kt
package com.obsidianbackup.performance

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Utilities for optimizing LazyColumn/LazyRow performance
 */

/**
 * Calculate optimal page size based on visible items
 */
fun LazyListState.getOptimalPageSize(): Int {
    val visibleItems = layoutInfo.visibleItemsInfo.size
    return (visibleItems * PAGE_SIZE_MULTIPLIER).toInt().coerceAtLeast(MIN_PAGE_SIZE)
}

/**
 * Check if list is near the end (for pagination)
 */
fun LazyListState.isNearEnd(threshold: Int = PAGINATION_THRESHOLD): Boolean {
    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    val totalItems = layoutInfo.totalItemsCount
    return lastVisibleIndex >= totalItems - threshold
}

/**
 * Observe scroll to end for pagination
 */
@Composable
fun LazyListState.observeScrollToEnd(
    threshold: Int = PAGINATION_THRESHOLD,
    onScrollToEnd: () -> Unit
) {
    val isNearEnd by remember {
        derivedStateOf {
            isNearEnd(threshold)
        }
    }
    
    LaunchedEffect(isNearEnd) {
        if (isNearEnd) {
            onScrollToEnd()
        }
    }
}

/**
 * Paged list state for efficient data loading
 */
class PagedListState<T>(
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val prefetchDistance: Int = PREFETCH_DISTANCE
) {
    private val _items = mutableStateListOf<T>()
    val items: List<T> = _items
    
    private var currentPage = 0
    private var isLoading = false
    private var hasMore = true
    
    suspend fun loadNextPage(loader: suspend (page: Int, size: Int) -> List<T>) {
        if (isLoading || !hasMore) return
        
        isLoading = true
        try {
            val newItems = loader(currentPage, pageSize)
            if (newItems.isEmpty()) {
                hasMore = false
            } else {
                _items.addAll(newItems)
                currentPage++
            }
        } finally {
            isLoading = false
        }
    }
    
    fun reset() {
        _items.clear()
        currentPage = 0
        hasMore = true
        isLoading = false
    }
    
    fun shouldLoadMore(lastVisibleIndex: Int): Boolean {
        return !isLoading && hasMore && lastVisibleIndex >= _items.size - prefetchDistance
    }
}

/**
 * Collect flow with pagination
 */
@Composable
fun <T> Flow<List<T>>.collectAsPagedState(
    pageSize: Int = DEFAULT_PAGE_SIZE
): State<List<T>> {
    return collectAsState(initial = emptyList())
}

/**
 * Optimized key function for LazyColumn items
 */
fun <T> stableKey(item: T, getId: (T) -> Any): Any {
    return getId(item)
}

/**
 * Content type provider for better item recycling
 */
inline fun <reified T> contentType(): String = T::class.java.simpleName

/**
 * Chunked items for better performance with large lists
 */
fun <T> List<T>.chunkedForLazyList(chunkSize: Int = CHUNK_SIZE): List<List<T>> {
    return chunked(chunkSize)
}

private const val DEFAULT_PAGE_SIZE = 20
private const val PREFETCH_DISTANCE = 5
private const val PAGINATION_THRESHOLD = 10
private const val PAGE_SIZE_MULTIPLIER = 1.5f
private const val MIN_PAGE_SIZE = 10
private const val CHUNK_SIZE = 50
