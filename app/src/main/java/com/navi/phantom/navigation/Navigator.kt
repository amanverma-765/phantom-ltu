package com.navi.phantom.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class Navigator(startDestination: Destination) {

    private val tabBackStacks = mutableMapOf<Destination, SnapshotStateList<Destination>>()
    private val _currentTab = mutableStateOf(startDestination)
    private val _isTabSwitch = mutableStateOf(false)

    val isTabSwitch: Boolean
        get() = _isTabSwitch.value


    val backStack: SnapshotStateList<Destination>
        get() = tabBackStacks.getOrPut(_currentTab.value) {
            mutableStateListOf(_currentTab.value)
        }

    fun navigateTo(destination: Destination) {
        _isTabSwitch.value = false
        backStack.add(destination)
    }

    fun goBack(): Boolean {
        _isTabSwitch.value = false
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    fun goBack(count: Int): Int {
        _isTabSwitch.value = false
        var popped = 0
        repeat(count) {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
                popped++
            }
        }
        return popped
    }

    fun popTo(destination: Destination): Boolean {
        _isTabSwitch.value = false
        while (backStack.size > 1 && backStack.lastOrNull() != destination) {
            backStack.removeLastOrNull()
        }
        return backStack.lastOrNull() == destination
    }

    fun popToRoot() {
        _isTabSwitch.value = false
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
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
                _isTabSwitch.value = false
                stack.clear()
                stack.add(tabRoot)
            }
            return
        }

        // Mark this as a tab switch for animation purposes
        _isTabSwitch.value = true

        // Switch to the new tab - its back stack is preserved
        _currentTab.value = tabRoot

        // Initialize the tab's back stack if it doesn't exist
        if (!tabBackStacks.containsKey(tabRoot)) {
            tabBackStacks[tabRoot] = mutableStateListOf(tabRoot)
        }
    }
}