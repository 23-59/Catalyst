package com.goldenminute.catalyst

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.goldenminute.catalyst.ui.TodoApp
import com.goldenminute.catalyst.ui.TodoViewModel
import com.goldenminute.catalyst.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-w1000dp-h1400dp")
class TodoAppRuntimeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @org.junit.Before
    fun setUp() {
        // Safe fallback
        java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    }

    @Test
    fun testAppLaunchAndRender() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TodoViewModel(application)
        
        composeTestRule.setContent {
            MyApplicationTheme {
                TodoApp(viewModel = viewModel)
            }
        }
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun testEditTask() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TodoViewModel(application)
        
        composeTestRule.setContent {
            MyApplicationTheme {
                TodoApp(viewModel = viewModel)
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Find add FAB and click it
        composeTestRule.onNodeWithTag("add_action_button", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Type task name and save
        composeTestRule.onNodeWithTag("add_task_title_input").performTextInput("Test Task")
        
        // Assert it is enabled
        composeTestRule.onNodeWithTag("confirm_add_task_button", useUnmergedTree = true).assertIsEnabled()
        
        // Use text match for the Add button
        composeTestRule.onNodeWithTag("confirm_add_task_button", useUnmergedTree = true).performClick()
        
        // Wait for the Task card with the title "Test Task" to be inserted and displayed
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithText("Test Task")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click the edit Task button
        composeTestRule.onNodeWithContentDescription("Edit Task", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
    }
}
