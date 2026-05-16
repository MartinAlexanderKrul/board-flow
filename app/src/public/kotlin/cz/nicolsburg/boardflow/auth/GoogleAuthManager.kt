package cz.nicolsburg.boardflow.auth

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import cz.nicolsburg.boardflow.model.LogEntry

class GoogleAuthManager(
    @Suppress("UNUSED_PARAMETER") context: Context
) {
    suspend fun signIn(
        previouslyAuthorizedEmail: String,
        onSignedIn: (Account) -> Unit,
        onLaunchAuthorization: (IntentSender) -> Unit,
        onLog: (title: String, detail: String, type: LogEntry.Type) -> Unit,
    ) {
        onLog("Google sync unavailable", "This edition does not include Google Sign-In.", LogEntry.Type.INFO)
    }

    fun restoreAuthorizationIfPossible(
        previouslyAuthorizedEmail: String,
        onSignedIn: (Account) -> Unit,
        onLog: (title: String, detail: String, type: LogEntry.Type) -> Unit,
    ) = Unit

    fun completeAuthorization(
        data: Intent?,
        onSignedIn: (Account) -> Unit,
        onLog: (title: String, detail: String, type: LogEntry.Type) -> Unit,
    ) {
        onLog("Google sync unavailable", "This edition does not include Google authorization.", LogEntry.Type.INFO)
    }

    fun signOut(account: Account, onComplete: () -> Unit) {
        onComplete()
    }

    suspend fun clearCredentialState() = Unit
}
