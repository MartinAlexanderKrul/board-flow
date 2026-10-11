package cz.nicolsburg.boardflow.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BggThingDetailJsonTest {

    @Test
    fun storedDetailsReadBackUnchanged() {
        val detail = BggApiClient.ThingDetail(
            objectid = "426129",
            name = "Dune: Imperium – Bloodlines",
            thumbnailUrl = "https://cf.geekdo-images.com/x.jpg",
            avgweight = "3.1",
            bggbestplayers = "3",
            bggrecplayers = "1,2,3,4",
            yearpublished = "2024",
            minplayers = "1",
            maxplayers = "4",
            playingtime = "120",
            type = "boardgameexpansion",
            baseGames = listOf(316554 to "Dune: Imperium", 397598 to "Dune: Imperium – Uprising")
        )
        assertEquals(detail, BggThingDetailJson.fromJson(BggThingDetailJson.toJson(detail)))
    }

    @Test
    fun aBaseGamesExpansionListIsKeptAndAMissingOneStaysUnknown() {
        val wingspan = BggApiClient.ThingDetail(
            objectid = "266192", name = "Wingspan", type = "boardgame",
            expansions = listOf(290837 to "Wingspan: Oceania Expansion")
        )
        assertEquals(wingspan, BggThingDetailJson.fromJson(BggThingDetailJson.toJson(wingspan)))
        assertNull(BggThingDetailJson.fromJson(BggThingDetailJson.toJson(BggApiClient.ThingDetail(objectid = "1")))?.expansions)
    }

    @Test
    fun missingThumbnailStaysNull() {
        val detail = BggApiClient.ThingDetail(objectid = "1", name = "Azul")
        assertNull(BggThingDetailJson.fromJson(BggThingDetailJson.toJson(detail))?.thumbnailUrl)
    }

    @Test
    fun brokenJsonIsIgnored() {
        assertNull(BggThingDetailJson.fromJson("{not json"))
    }
}
