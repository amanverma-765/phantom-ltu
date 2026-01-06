package com.riva.mods.core.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthEventBus {

    sealed class AuthEvent {
        data object SessionExpired : AuthEvent()
        data object LogoutRequested : AuthEvent()
    }

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun emitSessionExpired() {
        _events.tryEmit(AuthEvent.SessionExpired)
    }

    fun emitLogoutRequested() {
        _events.tryEmit(AuthEvent.LogoutRequested)
    }
}
