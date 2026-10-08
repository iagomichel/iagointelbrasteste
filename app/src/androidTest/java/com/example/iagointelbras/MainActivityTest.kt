package com.example.iagointelbras

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesTheCasaApplicationInterface() {
        val connectionVisible = composeRule.onAllNodesWithText("INTELBRAS / CASA INTELIGENTE")
            .fetchSemanticsNodes().isNotEmpty()
        val homeVisible = composeRule.onAllNodesWithText("Casa Inteligente")
            .fetchSemanticsNodes().isNotEmpty()

        assertTrue(connectionVisible || homeVisible)
    }
}
