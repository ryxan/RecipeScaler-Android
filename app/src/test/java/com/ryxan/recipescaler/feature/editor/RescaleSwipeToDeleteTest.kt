package com.ryxan.recipescaler.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import com.ryxan.recipescaler.core.designsystem.RescaleSwipeToDelete
import com.ryxan.recipescaler.core.designsystem.RescaleTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RescaleSwipeToDeleteTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `bare swipe to delete`() {
        var deleted = false
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                Column {
                    RescaleSwipeToDelete(onDelete = { deleted = true }) {
                        Text("row", Modifier.fillMaxWidth().height(48.dp))
                    }
                }
            }
        }
        composeRule.onNodeWithText("row").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertTrue(deleted)
    }

    @Test
    fun `swipe to delete inside lazy column`() {
        var deleted = false
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                LazyColumn {
                    items(listOf(1, 2), key = { it }) { i ->
                        RescaleSwipeToDelete(onDelete = { deleted = true }) {
                            Text("row$i", Modifier.fillMaxWidth().height(48.dp))
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("row1").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertTrue(deleted)
    }

    @Test
    fun `swipe to delete inside reorderable`() {
        var deleted = false
        composeRule.setContent {
            RescaleTheme(dynamicColor = false) {
                val listState = rememberLazyListState()
                val reorder = rememberReorderableLazyListState(listState) { _, _ -> }
                LazyColumn(state = listState) {
                    items(listOf(1, 2), key = { it }) { i ->
                        ReorderableItem(reorder, key = i) {
                            RescaleSwipeToDelete(onDelete = { deleted = true }) {
                                Text("row$i", Modifier.fillMaxWidth().height(48.dp))
                            }
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("row1").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertTrue(deleted)
    }
}
