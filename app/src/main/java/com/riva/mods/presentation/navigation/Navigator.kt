package com.riva.mods.presentation.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class Navigator(private val startDestination: Screen) {

    private val tabBackStacks = mutableMapOf<Screen, SnapshotStateList<Screen>>()

    private val _currentTab = mutableStateOf(startDestination)


    val backStack: SnapshotStateList<Screen>
        get() = tabBackStacks.getOrPut(_currentTab.value) {
            mutableStateListOf(_currentTab.value)
        }

    fun navigateTo(destination: Screen) {
        backStack.add(destination)
    }

    fun goBack(): Boolean {
        // Don't pop if we're at the tab root (only 1 item in stack)
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    fun currentScreen(): Screen? {
        return backStack.lastOrNull()
    }

    fun currentTab(): Screen {
        return _currentTab.value
    }

    fun switchTab(tabRoot: Screen) {
        if (_currentTab.value == tabRoot) return

        // Switch to the new tab - its back stack is preserved
        _currentTab.value = tabRoot

        // Initialize the tab's back stack if it doesn't exist
        if (!tabBackStacks.containsKey(tabRoot)) {
            tabBackStacks[tabRoot] = mutableStateListOf(tabRoot)
        }
    }
}