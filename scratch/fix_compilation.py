# Fix 1: StudioViewModel.kt - add VideoQuality import
vm_path = "android/app/src/main/java/com/mrpallanti/viralvideo/presentation/viewmodel/StudioViewModel.kt"
with open(vm_path, "r", encoding="utf-8") as f:
    vm_content = f.read()

if "import com.mrpallanti.viralvideo.domain.model.VideoQuality" not in vm_content:
    vm_content = vm_content.replace(
        "import com.mrpallanti.viralvideo.domain.model.VideoMetadata\n",
        "import com.mrpallanti.viralvideo.domain.model.VideoMetadata\nimport com.mrpallanti.viralvideo.domain.model.VideoQuality\n"
    )
    with open(vm_path, "w", encoding="utf-8") as f:
        f.write(vm_content)
    print("Fixed StudioViewModel.kt imports")

# Fix 2: StudioScreen.kt - add horizontalScroll and rememberScrollState imports
screen_path = "android/app/src/main/java/com/mrpallanti/viralvideo/presentation/ui/StudioScreen.kt"
with open(screen_path, "r", encoding="utf-8") as f:
    screen_content = f.read()

if "import androidx.compose.foundation.horizontalScroll" not in screen_content:
    screen_content = screen_content.replace(
        "import androidx.compose.foundation.clickable\n",
        "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.horizontalScroll\nimport androidx.compose.foundation.rememberScrollState\n"
    )
    with open(screen_path, "w", encoding="utf-8") as f:
        f.write(screen_content)
    print("Fixed StudioScreen.kt imports")

# Fix 3: CustomClipperView.kt - replace FilterChipDefaults.filterChipBorder with BorderStroke
clipper_path = "android/app/src/main/java/com/mrpallanti/viralvideo/presentation/ui/components/CustomClipperView.kt"
with open(clipper_path, "r", encoding="utf-8") as f:
    clipper_content = f.read()

old_border = """                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) AccentCyan else BorderSubtle,
                                selectedBorderColor = AccentCyan,
                                borderWidth = if (isSelected) 1.5.dp else 1.dp
                            )"""

new_border = """                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentCyan else BorderSubtle
                            )"""

if old_border in clipper_content:
    clipper_content = clipper_content.replace(old_border, new_border)
    with open(clipper_path, "w", encoding="utf-8") as f:
        f.write(clipper_content)
    print("Fixed CustomClipperView.kt border")
