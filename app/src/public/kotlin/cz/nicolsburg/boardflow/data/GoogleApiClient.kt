package cz.nicolsburg.boardflow.data

import android.accounts.Account
import android.content.Context
import cz.nicolsburg.boardflow.SyncConfig
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.model.SpreadsheetDetails

class GoogleApiClient(
    context: Context,
    account: Account,
    private val spreadsheetId: String,
    private val sheetTabName: String = SyncConfig.SHEET_TAB_NAME
) {
    fun getSpreadsheetDetails(): SpreadsheetDetails = unavailable()
    fun writeHeaderRow(headers: List<String>): Unit = unavailable()
    fun applyDefaultSheetStyle(headers: List<String>): Unit = unavailable()
    fun readHeaderMap(): Map<String, Int> = unavailable()
    fun readAllColumns(): List<List<Any>> = unavailable()
    fun readCollectionRows(): List<GameItem> = unavailable()
    fun readGameRows(): List<GameRow> = unavailable()
    fun writeCsvRow(rowIndex: Int, csvRow: Map<String, String>, headerMap: Map<String, Int>, existingRow: List<Any>): Unit = unavailable()
    fun writeBggRow(rowIndex: Int, game: BggApiClient.BggGame, headerMap: Map<String, Int>, existingRow: List<Any>): Unit = unavailable()
    fun writeResultToRow(rowIndex: Int, shareUrl: String, qrFileUrl: String): Unit = unavailable()
    fun writeSleevesJsonByObjectId(games: List<GameItem>): Int = unavailable()
    fun insertRowAfterHeader(): Int = unavailable()
    fun createSharedFolder(title: String): String = unavailable()
    fun uploadQr(gameName: String, qrPng: ByteArray): Unit = unavailable()
    fun getLastQrFileUrl(): String = unavailable()
    fun downloadQrBytes(): ByteArray? = unavailable()

    private fun unavailable(): Nothing =
        throw UnsupportedOperationException("Google Sheets and Drive sync are not available in this edition.")

    companion object {
        fun createSpreadsheet(
            context: Context,
            account: Account,
            title: String,
            sheetTitle: String,
            headers: List<String>
        ): SpreadsheetDetails =
            throw UnsupportedOperationException("Google Sheets sync is not available in this edition.")
    }

    data class GameRow(val rowIndex: Int, val gameName: String, val shareUrl: String)
}
