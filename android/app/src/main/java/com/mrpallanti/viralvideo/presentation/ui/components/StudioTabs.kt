package com.mrpallanti.viralvideo.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrpallanti.viralvideo.presentation.ui.theme.*
import com.mrpallanti.viralvideo.presentation.viewmodel.StudioTab

@Composable
fun StudioBottomNavigationBar(
    currentTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit,
    recentCount: Int = 0
) {
    NavigationBar(
        containerColor = BgDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .border(
                width = 1.dp,
                color = BorderSubtle,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        NavigationBarItem(
            selected = currentTab == StudioTab.STUDIO,
            onClick = { onTabSelected(StudioTab.STUDIO) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Studio",
                    tint = if (currentTab == StudioTab.STUDIO) PrimaryPurple else TextSecondary
                )
            },
            label = {
                Text(
                    text = "AI Studio",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.STUDIO) FontWeight.Bold else FontWeight.Normal,
                    color = if (currentTab == StudioTab.STUDIO) PrimaryPurple else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PrimaryPurple.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = currentTab == StudioTab.CLIPPER,
            onClick = { onTabSelected(StudioTab.CLIPPER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Custom Clipper",
                    tint = if (currentTab == StudioTab.CLIPPER) PrimaryPurple else TextSecondary
                )
            },
            label = {
                Text(
                    text = "Clipper",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.CLIPPER) FontWeight.Bold else FontWeight.Normal,
                    color = if (currentTab == StudioTab.CLIPPER) PrimaryPurple else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PrimaryPurple.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = currentTab == StudioTab.BRANDING,
            onClick = { onTabSelected(StudioTab.BRANDING) },
            icon = {
                Icon(
                    imageVector = Icons.Default.BrandingWatermark,
                    contentDescription = "Branding",
                    tint = if (currentTab == StudioTab.BRANDING) PrimaryPurple else TextSecondary
                )
            },
            label = {
                Text(
                    text = "Branding",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.BRANDING) FontWeight.Bold else FontWeight.Normal,
                    color = if (currentTab == StudioTab.BRANDING) PrimaryPurple else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PrimaryPurple.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = currentTab == StudioTab.LIBRARY,
            onClick = { onTabSelected(StudioTab.LIBRARY) },
            icon = {
                BadgedBox(
                    badge = {
                        if (recentCount > 0) {
                            Badge(
                                containerColor = PrimaryPurple,
                                contentColor = Color.White
                            ) {
                                Text("$recentCount", fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Library",
                        tint = if (currentTab == StudioTab.LIBRARY) PrimaryPurple else TextSecondary
                    )
                }
            },
            label = {
                Text(
                    text = "Library",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.LIBRARY) FontWeight.Bold else FontWeight.Normal,
                    color = if (currentTab == StudioTab.LIBRARY) PrimaryPurple else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PrimaryPurple.copy(alpha = 0.15f)
            )
        )
    }
}
