package com.goldenminute.catalyst

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HyperTask", appName)
  }

  @Test
  fun `verify persian localization strings`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Create a localized configuration for Persian
    val locale = Locale("fa")
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration).apply {
      setLocale(locale)
    }
    val faContext = context.createConfigurationContext(config)
    
    val appNameFa = faContext.getString(R.string.app_name)
    assertEquals("لیست کارها", appNameFa)

    val tasksDoneFa = faContext.getString(R.string.tasks_done, 2, 5)
    assertNotNull(tasksDoneFa)
    
    val dueDateFa = faContext.getString(R.string.due_date_label, "12:30 PM")
    assertNotNull(dueDateFa)
  }

  @Test
  fun `verify main activity launches successfully`() {
    androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      assertNotNull(scenario)
      scenario.onActivity { activity ->
        val viewModel = androidx.lifecycle.ViewModelProvider(activity)[com.goldenminute.catalyst.ui.TodoViewModel::class.java]
        // Change language to Persian
        viewModel.setLanguage("fa")
        // Change language back to English
        viewModel.setLanguage("en")
        // Just verify no exception was thrown and language state is set
        assertEquals("en", viewModel.appLanguage.value)
      }
    }
  }
}
