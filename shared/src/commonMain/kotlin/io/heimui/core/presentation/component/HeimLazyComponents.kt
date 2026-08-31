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

@Composable
fun HeimLazyColumnRenderer(
    component: LazyColumnComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Pagination detection
    component.pagination?.let { pagination ->
        if (pagination.hasMore) {
            val shouldLoadMore by remember {
                derivedStateOf {
                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = listState.layoutInfo.totalItemsCount
                    totalItems > 0 && lastVisibleItemIndex >= (totalItems - pagination.loadThreshold)
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) {
                    pagination.onLoadMoreActions.forEach { action ->
                        onAction(action)
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y),
        contentPadding = PaddingValues(component.padding.dp),
        verticalArrangement = Arrangement.spacedBy(component.spacing.dp)
    ) {
        itemsIndexed(
            items = component.items,
            key = { _, item -> item.id }
        ) { _, child ->
            HeimRenderer(
                component = child,
                stateManager = stateManager,
                onAction = onAction
            )
        }
    }
}

@Composable
fun HeimLazyRowRenderer(
    component: LazyRowComponent,
    stateManager: HeimStateManager,
    onAction: (HeimAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    component.pagination?.let { pagination ->
        if (pagination.hasMore) {
            val shouldLoadMore by remember {
                derivedStateOf {
                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val totalItems = listState.layoutInfo.totalItemsCount
                    totalItems > 0 && lastVisibleItemIndex >= (totalItems - pagination.loadThreshold)
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) {
                    pagination.onLoadMoreActions.forEach { action ->
                        onAction(action)
                    }
                }
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .heimAccessibility(component.a11y),
        contentPadding = PaddingValues(component.padding.dp),
        horizontalArrangement = Arrangement.spacedBy(component.spacing.dp)
    ) {
        itemsIndexed(
            items = component.items,
            key = { _, item -> item.id }
        ) { _, child ->
            HeimRenderer(
                component = child,
                stateManager = stateManager,
                onAction = onAction
            )
        }
    }
}
