package io.heimui.core.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.component.LazyColumnComponent
import io.heimui.core.domain.model.component.LazyRowComponent
import io.heimui.core.presentation.HeimRenderer
import io.heimui.core.presentation.accessibility.heimAccessibility
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.core.presentation.component.LocalInsideVerticalScroller
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
internal fun HeimLazyColumnRenderer(
    component: LazyColumnComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Pagination detection
    val pagination = component.pagination
    if (pagination != null && pagination.hasMore) {
        // Keyed on `pagination`: without it the derived state froze the first page's threshold
        // and cursor, so page 2 never loaded.
        var requestedCursor by remember(pagination.nextCursor) { mutableStateOf<String?>(null) }
        val shouldLoadMore by remember(pagination, listState) {
            derivedStateOf {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
                val total = listState.layoutInfo.totalItemsCount
                total > 0 && last >= total - pagination.loadThreshold.coerceAtLeast(1)
            }
        }
        LaunchedEffect(shouldLoadMore, pagination.nextCursor) {
            // The cursor guard de-duplicates: a scroll bounce used to fire two concurrent
            // page requests for the same cursor.
            if (shouldLoadMore && requestedCursor != pagination.nextCursor) {
                requestedCursor = pagination.nextCursor
                pagination.onLoadMoreActions.forEach { onAction(it) }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id),
        contentPadding = component.padding.toPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(component.spacing.dp)
    ) {
        itemsIndexed(
            items = component.items,
            key = { _, item -> item.id }
        ) { _, child ->
            CompositionLocalProvider(LocalInsideVerticalScroller provides true) {
                HeimRenderer(
                    component = child,
                    stateManager = stateManager,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
internal fun HeimLazyRowRenderer(
    component: LazyRowComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val pagination = component.pagination
    if (pagination != null && pagination.hasMore) {
        // Keyed on `pagination`: without it the derived state froze the first page's threshold
        // and cursor, so page 2 never loaded.
        var requestedCursor by remember(pagination.nextCursor) { mutableStateOf<String?>(null) }
        val shouldLoadMore by remember(pagination, listState) {
            derivedStateOf {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
                val total = listState.layoutInfo.totalItemsCount
                total > 0 && last >= total - pagination.loadThreshold.coerceAtLeast(1)
            }
        }
        LaunchedEffect(shouldLoadMore, pagination.nextCursor) {
            // The cursor guard de-duplicates: a scroll bounce used to fire two concurrent
            // page requests for the same cursor.
            if (shouldLoadMore && requestedCursor != pagination.nextCursor) {
                requestedCursor = pagination.nextCursor
                pagination.onLoadMoreActions.forEach { onAction(it) }
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y, componentId = component.id),
        contentPadding = component.padding.toPaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(component.spacing.dp)
    ) {
        itemsIndexed(
            items = component.items,
            key = { _, item -> item.id }
        ) { _, child ->
            CompositionLocalProvider(LocalInsideVerticalScroller provides true) {
                HeimRenderer(
                    component = child,
                    stateManager = stateManager,
                    onAction = onAction
                )
            }
        }
    }
}
