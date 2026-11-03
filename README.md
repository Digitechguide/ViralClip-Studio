# ViralClip Studio ⚡
> Turn Long Videos into High-Retention Viral Shorts & Reels with Gemini AI.

[![Architecture & Flow](https://img.shields.io/badge/Architecture-PROJECT__ARCHITECTURE__AND__FLOW.md-blueviolet?style=for-the-badge)](PROJECT_ARCHITECTURE_AND_FLOW.md)
[![Latest Release](https://img.shields.io/badge/Release-v1.0.50-success?style=for-the-badge)](https://viral.mrpallanti.in/ViralClip-Studio-v1.0.50.apk)
[![Live Production](https://img.shields.io/badge/Live-viral.mrpallanti.in-cyan?style=for-the-badge)](https://viral.mrpallanti.in/)

> 📖 **Developer / AI Memory & System Flow:** See [PROJECT_ARCHITECTURE_AND_FLOW.md](PROJECT_ARCHITECTURE_AND_FLOW.md) for full architectural blueprints, data flow diagrams, clean architecture specifications, CI/CD pipeline, and deployment instructions.

---

## 🌟 Key Features

1. **AI Viral Hook & Segment Finder**:
   - Analyzes video content, title, and context using **Google Gemini 1.5 Flash** (Free tier).
   - Identifies high-retention segments with virality scores (e.g. 96%), retention hooks, hashtags, and why the segment works.
   - **One-click "Apply Segment"**: snaps the trimmer timeline and seeks the video directly to the recommended viral clip.

2. **Interactive Duration Trimmer**:
   - Dual-handle draggable timeline trimmer for exact Start and End points.
   - Quick duration presets: **15s**, **30s**, **45s**, **60s**, or custom second inputs.
   - Live scrubber with real-time clip duration readouts.

3. **9:16 Vertical Video Studio (Shorts / Reels / TikTok)**:
   - Realistic smartphone viewport preview.
   - **Smart Framing Modes**: Center (Speaker focus), Left, Right, or Fit (with blurred background).
   - Aspect ratio switcher: `9:16` (Shorts), `1:1` (Instagram Post), `16:9` (Landscape).

4. **Viral Caption & Hook Styler**:
   - Customizable on-screen text overlays (e.g., `WAIT FOR THIS PART... 🤯`).
   - 4 creator styles: **MrBeast Yellow/Black**, **Red Alert**, **Neon Cyan**, **Clean White**.
   - Position selector: Top, Center, Bottom.

5. **Client-Side Export & Download**:
   - Fast client-side rendering & recording via Canvas + MediaRecorder API.
   - Works on desktop & mobile browsers without bogging down your shared hosting server.
   - Exports high-quality `.mp4` or `.webm` ready to upload.

---

## 🚀 Quick Local Testing (On Your PC)

You can launch and test the studio immediately using Node.js:

```bash
# In the project directory:
node server.js
```
Open your browser and navigate to:
```
http://localhost:3000
```
- Click **"Try Demo Clip"** to test with pre-loaded videos.
- Or paste a YouTube URL / drag-and-drop a local video file.

---

## 🌐 Deploying to Serverbyt.in (PHP Shared Hosting)

Your `serverbyt.in` shared hosting with PHP is fully supported! Follow these steps:

### Step 1: Upload Files
Upload all files and folders to your domain's public root (`public_html/` or a subfolder like `public_html/shorts/`):
```
public_html/
├── index.html
├── css/
│   └── style.css
├── js/
│   ├── app.js
│   ├── video-engine.js
│   ├── ai-analyzer.js
│   └── youtube-helper.js
└── api/
    ├── config.php
    ├── info.php
    ├── download.php
    └── suggest.php
```

### Step 2: Configure Free Gemini API Key (Optional)
1. Get a free API key at [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Either:
   - Edit `api/config.php` and replace `'YOUR_GEMINI_API_KEY_HERE'` with your key.
   - OR click the **Settings** gear icon directly in the web app and paste your key there (stored securely in your browser's localStorage).

### Step 3: Enable Server YouTube Downloader (Optional)
If you want the PHP server to directly download YouTube videos via yt-dlp:
1. Download the standalone Linux `yt-dlp` binary:
   ```bash
   curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o yt-dlp
   ```
2. Upload `yt-dlp` to your `public_html/api/` folder.
3. In cPanel File Manager or via FTP/SSH, set permissions to `755` (`chmod +x yt-dlp`).

*(Note: Even without `yt-dlp`, users can upload their own video files directly or use demo footage with full AI hook suggestions and trimming functionality!)*

---

## 📱 Web-based vs. Android App Comparison

| Criteria | Web App (This Build) | Android Native App |
| :--- | :--- | :--- |
| **Hosting Compatibility** | Runs on `serverbyt.in` shared hosting | Needs APK build / Play Store distribution |
| **User Access** | Instant link access on phone, tablet, or PC | Requires download & installation |
| **Resource Usage** | Trimming & rendering done client-side | Heavy local CPU / GPU processing |
| **Maintenance** | 1 central update updates all users | App updates require re-installing APK |

**Recommendation:** The Web-based Studio deployed to `serverbyt.in` gives your users the quickest, friction-free experience with zero installs.
