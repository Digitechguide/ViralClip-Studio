package com.mrpallanti.viralvideo

import com.mrpallanti.viralvideo.domain.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NewFeaturesUnitTest {

    @Test
    fun testEditorialStyleEnumValuesAndMetadata() {
        val styles = EditorialStyle.values()
        assertEquals(5, styles.size)

        // Verify all 5 AutoClip curation styles exist
        val classic = EditorialStyle.valueOf("CLASSIC")
        val editorial = EditorialStyle.valueOf("EDITORIAL")
        val street = EditorialStyle.valueOf("STREET")
        val news = EditorialStyle.valueOf("NEWS")
        val cinema = EditorialStyle.valueOf("CINEMA")

        assertEquals("Classic Clean", classic.label)
        assertEquals("Magazine Editorial", editorial.label)
        assertEquals("Street Kinetic", street.label)
        assertEquals("News Headline", news.label)
        assertEquals("Cinematic Letterbox", cinema.label)

        assertEquals("Street", street.shortLabel)
        assertEquals("Editorial", editorial.shortLabel)
        assertEquals("News", news.shortLabel)
        assertEquals("Cinema", cinema.shortLabel)
    }

    @Test
    fun testOpenShortsCropPlacements() {
        val split = CropPlacement.valueOf("SPLIT_PODCAST")
        val pip = CropPlacement.valueOf("SCREENCAST_PIP")

        assertNotNull(split)
        assertNotNull(pip)

        val allPlacements = CropPlacement.values()
        assertTrue(allPlacements.contains(CropPlacement.SPLIT_PODCAST))
        assertTrue(allPlacements.contains(CropPlacement.SCREENCAST_PIP))
        assertTrue(allPlacements.contains(CropPlacement.AUTO_FACE))
        assertTrue(allPlacements.contains(CropPlacement.CENTER))
    }

    @Test
    fun testOverlayConfigDefaultsAndPunchIn() {
        val defaultConfig = OverlayConfig()

        // Verify defaults
        assertEquals(EditorialStyle.CLASSIC, defaultConfig.editorialStyle)
        assertFalse(defaultConfig.enablePunchIn)
        assertEquals(1.12f, defaultConfig.punchInZoom, 0.001f)
        assertEquals(CropPlacement.AUTO_FACE, defaultConfig.cropPlacement)

        // Verify punch-in activation & configuration
        val activePunchConfig = defaultConfig.copy(
            enablePunchIn = true,
            punchInZoom = 1.15f,
            editorialStyle = EditorialStyle.STREET,
            cropPlacement = CropPlacement.SPLIT_PODCAST
        )

        assertTrue(activePunchConfig.enablePunchIn)
        assertEquals(1.15f, activePunchConfig.punchInZoom, 0.001f)
        assertEquals(EditorialStyle.STREET, activePunchConfig.editorialStyle)
        assertEquals(CropPlacement.SPLIT_PODCAST, activePunchConfig.cropPlacement)
    }

    @Test
    fun testSocialPackageDataModel() {
        val socialPkg = SocialPackage(
            tiktokTitle = "Wait till you see what happens first 🤯",
            tiktokHashtags = listOf("#fyp", "#viral", "#foryou"),
            reelsTitle = "The truth is finally out 👇",
            reelsHashtags = listOf("#reels", "#explore", "#creator"),
            shortsTitle = "Unbelievable First 30 Seconds #Shorts",
            shortsHashtags = listOf("#Shorts", "#youtubeshorts")
        )

        assertEquals("Wait till you see what happens first 🤯", socialPkg.tiktokTitle)
        assertTrue(socialPkg.tiktokHashtags.contains("#fyp"))
        assertTrue(socialPkg.reelsHashtags.contains("#reels"))
        assertTrue(socialPkg.shortsHashtags.contains("#Shorts"))
    }

    @Test
    fun testViralClipWithNewFeaturesModel() {
        val clip = ViralClip(
            id = 1,
            title = "💥 Strongest Opening Hook",
            startSeconds = 0f,
            endSeconds = 30f,
            duration = 30f,
            viralityScore = 95,
            hookReason = "High retention hook",
            captionOverlay = "YOU WON'T BELIEVE THIS... 🤯",
            hashtags = listOf("#Viral", "#Shorts"),
            recommendedLayout = "split_podcast",
            editorialStyle = "street",
            emphasisTimes = listOf(3.5f, 12.0f),
            socialPackage = SocialPackage(
                tiktokTitle = "Viral TikTok Hook",
                tiktokHashtags = listOf("#fyp"),
                reelsTitle = "Viral Reel",
                reelsHashtags = listOf("#reels"),
                shortsTitle = "Viral Short",
                shortsHashtags = listOf("#Shorts")
            )
        )

        assertEquals("split_podcast", clip.recommendedLayout)
        assertEquals("street", clip.editorialStyle)
        assertEquals(2, clip.emphasisTimes.size)
        assertEquals(3.5f, clip.emphasisTimes[0], 0.01f)
        assertNotNull(clip.socialPackage)
        assertEquals("Viral TikTok Hook", clip.socialPackage?.tiktokTitle)
    }

    @Test
    fun testGeminiFlashApiResponseParsing() {
        val mockApiResponse = """
        {
            "status": "success",
            "data": {
                "clips": [
                    {
                        "id": 1,
                        "title": "💥 Strongest Opening Hook",
                        "start_seconds": 0,
                        "end_seconds": 30,
                        "duration": 30,
                        "virality_score": 95,
                        "hook_reason": "High retention first impression.",
                        "caption_overlay": "YOU WON'T BELIEVE THIS... 🤯",
                        "recommended_layout": "split_podcast",
                        "editorial_style": "street",
                        "emphasis_seconds": [3.5, 12.0],
                        "hashtags": ["#Viral", "#Shorts", "#Trending"],
                        "social_package": {
                            "tiktok": {
                                "title": "Wait till you see what happens first 🤯",
                                "hashtags": ["#fyp", "#viral", "#foryou"]
                            },
                            "reels": {
                                "title": "The truth is finally out 👇",
                                "hashtags": ["#reels", "#explore"]
                            },
                            "shorts": {
                                "title": "Unbelievable First 30 Seconds #Shorts",
                                "hashtags": ["#Shorts", "#youtubeshorts"]
                            }
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val json = JSONObject(mockApiResponse)
        val clip = json.getJSONObject("data").getJSONArray("clips").getJSONObject(0)

        assertEquals("split_podcast", clip.getString("recommended_layout"))
        assertEquals("street", clip.getString("editorial_style"))

        val emphasisArr = clip.getJSONArray("emphasis_seconds")
        assertEquals(2, emphasisArr.length())
        assertEquals(3.5, emphasisArr.getDouble(0), 0.01)
        assertEquals(12.0, emphasisArr.getDouble(1), 0.01)

        val socialObj = clip.getJSONObject("social_package")
        val tiktokObj = socialObj.getJSONObject("tiktok")
        assertEquals("Wait till you see what happens first 🤯", tiktokObj.getString("title"))

        val tiktokTags = mutableListOf<String>()
        val tagsArr = tiktokObj.getJSONArray("hashtags")
        for (i in 0 until tagsArr.length()) {
            tiktokTags.add(tagsArr.getString(i))
        }
        assertTrue(tiktokTags.contains("#fyp"))
    }
}
