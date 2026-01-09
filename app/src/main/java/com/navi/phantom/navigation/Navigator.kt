package com.navi.phantom.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.collections.removeLastOrNull

class Navigator(startDestination: Destination) {

    private val tabBackStacks = mutableMapOf<Destination, SnapshotStateList<Destination>>()
    private val _currentTab = mutableStateOf(startDestination)


    val backStack: SnapshotStateList<Destination>
        get() = tabBackStacks.getOrPut(_currentTab.value) {
            mutableStateListOf(_currentTab.value)
        }

    fun navigateTo(destination: Destination) {
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

    fun currentScreen(): Destination? {
        return backStack.lastOrNull()
    }

    fun currentTab(): Destination {
        return _currentTab.value
    }

    fun switchTab(tabRoot: Destination) {
        if (_currentTab.value == tabRoot) {
            // Clear the tab's backstack when clicking the same tab again
            val stack = tabBackStacks[tabRoot]
            if (stack != null && stack.size > 1) {
                stack.clear()
                stack.add(tabRoot)
            }
            return
        }

        // Switch to the new tab - its back stack is preserved
        _currentTab.value = tabRoot

        // Initialize the tab's back stack if it doesn't exist
        if (!tabBackStacks.containsKey(tabRoot)) {
            tabBackStacks[tabRoot] = mutableStateListOf(tabRoot)
        }
    }
}