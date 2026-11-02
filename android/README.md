# 📱 ViralClip Studio — Native Android Application

An enterprise-grade, offline-capable native Android application built with **Clean Architecture** (Kotlin + Jetpack Compose) for converting YouTube videos and long videos into viral 9:16 Shorts, Instagram Reels, and TikToks.

Designed and engineered under the **`mrpallanticeo`** architectural standard.

---

## 🚀 Key Architectural Advantages (Zero Server / Zero Colab)

1. **100% On-Device Video Rendering (FFmpeg-Kit GPL):**
   - The phone's own CPU & GPU perform the 9:16 vertical cropping, exact timestamp trimming, and hook banner overlay burning.
   - Saves final videos directly into the phone's **Gallery / Photos (`Movies/ViralClips`)** via Android MediaStore.
2. **No CORS / Browser Security Limitations:**
   - Extracts YouTube video streams directly on mobile without iframes.
3. **AI Virality Engine (Gemini Flash):**
   - Direct, ultra-fast API connection to Google Gemini Flash for retention curves, viral hook timestamps, titles, and hashtags.
4. **Android Share Sheet Integration:**
   - Users can share any video directly from the official **YouTube app $\rightarrow$ Share $\rightarrow$ ViralClip Studio** to automatically analyze and crop clips in 1 click!

---

## 🏛️ Clean Architecture Structure

```
android/app/src/main/java/in/mrpallanti/viralvideo/
├── ViralVideoApp.kt                     # Application entry point & DI container
├── domain/                              # Core Business Logic (Zero Framework Dependencies)
│   ├── model/
│   │   ├── VideoMetadata.kt            # Video model
│   │   ├── ViralClip.kt                # AI suggested hook segments & virality scores
│   │   ├── TrimRange.kt                # Start & End timestamps
│   │   └── RenderJob.kt                # Hardware rendering job state
│   ├── repository/
│   │   ├── VideoStreamRepository.kt     # YouTube extraction interface
│   │   ├── AiViralityRepository.kt      # Gemini AI analysis interface
│   │   └── VideoRendererRepository.kt   # FFmpeg hardware renderer interface
│   └── usecase/
│       ├── ExtractYoutubeStreamUseCase.kt
│       ├── AnalyzeViralHooksUseCase.kt
│       └── RenderVerticalShortUseCase.kt
├── data/                                # Hardware & Remote Implementations
│   ├── remote/
│   │   ├── YoutubeStreamExtractorImpl.kt  # YouTube stream & oEmbed metadata
│   │   └── GeminiFlashApiClient.kt        # Gemini Flash REST API + Smart Fallback
│   └── local/
│       └── FfmpegVideoRendererImpl.kt     # Native FFmpegKit 9:16 cropper & MediaStore saver
└── presentation/                        # Reactive UI (Jetpack Compose)
    ├── MainActivity.kt                  # Android Activity & Share Sheet handler
    ├── viewmodel/
    │   └── StudioViewModel.kt           # StateFlow UI state management
    └── ui/
        ├── theme/                       # Modern dark cyberpunk UI theme
        └── StudioScreen.kt              # 9:16 mobile mockup, trimmer & hook cards
```

---

## 🛠️ How to Build the APK

### Option A: 1-Click Cloud Build (GitHub Actions)
A pre-configured CI workflow is ready at `.github/workflows/build-apk.yml`.
1. Push this project to GitHub.
2. Go to **Actions** $\rightarrow$ **Build ViralClip Android APK**.
3. Download the generated `ViralClip-Studio-Debug-APK` directly to your phone!

### Option B: Local Build with Android Studio
1. Open **Android Studio**.
2. Select **Open an existing project** $\rightarrow$ choose the `android` folder.
3. Let Gradle sync dependencies.
4. Click **Run** (`Shift + F10`) or **Build $\rightarrow$ Build Bundle(s) / APK(s) $\rightarrow$ Build APK(s)**.
