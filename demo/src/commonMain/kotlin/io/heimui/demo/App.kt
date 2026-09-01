package io.heimui.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.action.ShowSnackbarAction
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.data.repository.MockHeimScreenRepository
import io.heimui.core.presentation.HeimScreen
import io.heimui.core.presentation.designsystem.HeimTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    HeimTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        val currentScreenId = DemoScreens.screens[selectedTabIndex].first

        val mockRepository = remember {
            MockHeimScreenRepository(
                jsonProvider = { screenId -> DemoScreens.getJson(screenId) },
                simulatedDelayMillis = 200L
            )
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "HeimUI Explorer",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DemoScreens.screens.forEachIndexed { index, (_, label) ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(text = label) }
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                HeimScreen(
                    screenId = currentScreenId,
                    repository = mockRepository,
                    onAction = { action ->
                        when (action) {
                            is ShowSnackbarAction -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(action.message)
                                }
                            }
                            is SubmitFormAction -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Form submitted to ${action.endpoint}")
                                }
                            }
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}
