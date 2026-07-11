package com.example.ui.viewmodel

sealed interface TokenVerificationState {
    object Idle : TokenVerificationState
    object Verifying : TokenVerificationState
    object Success : TokenVerificationState
    data class Error(val message: String) : TokenVerificationState
}
