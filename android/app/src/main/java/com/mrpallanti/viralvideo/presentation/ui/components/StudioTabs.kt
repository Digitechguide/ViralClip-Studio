package com.mrpallanti.viralvideo.presentation.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
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
        containerColor = Color(0xFF070B14),
        tonalElevation = 10.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            BorderSubtle,
                            PrimaryPurple.copy(alpha = 0.5f),
                            AccentCyan.copy(alpha = 0.3f),
                            BorderSubtle
                        )
                    )
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        NavigationBarItem(
            selected = currentTab == StudioTab.STUDIO,
            onClick = { onTabSelected(StudioTab.STUDIO) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Studio",
                    tint = if (currentTab == StudioTab.STUDIO) PrimaryPurple else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Studio",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.STUDIO) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (currentTab == StudioTab.STUDIO) Color.White else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PrimaryPurple.copy(alpha = 0.22f)
            )
        )

        NavigationBarItem(
            selected = currentTab == StudioTab.CLIPPER,
            onClick = { onTabSelected(StudioTab.CLIPPER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Custom Clipper",
                    tint = if (currentTab == StudioTab.CLIPPER) AccentCyan else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Clipper",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.CLIPPER) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (currentTab == StudioTab.CLIPPER) Color.White else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = AccentCyan.copy(alpha = 0.22f)
            )
        )

        NavigationBarItem(
            selected = currentTab == StudioTab.BRANDING,
            onClick = { onTabSelected(StudioTab.BRANDING) },
            icon = {
                Icon(
                    imageVector = Icons.Default.BrandingWatermark,
                    contentDescription = "Branding",
                    tint = if (currentTab == StudioTab.BRANDING) PrimaryPurple else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            },
            label = {
                Text(
                    text = "Branding",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.BRANDING) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (currentTab == StudioTab.BRANDING) Color.White else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = PrimaryPurple.copy(alpha = 0.22f)
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
                                Text("$recentCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Library",
                        tint = if (currentTab == StudioTab.LIBRARY) SuccessGreen else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            label = {
                Text(
                    text = "Library",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == StudioTab.LIBRARY) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (currentTab == StudioTab.LIBRARY) Color.White else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = SuccessGreen.copy(alpha = 0.22f)
            )
        )
    }
}
