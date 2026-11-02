/**
 * Main Application Controller
 * Orchestrates YouTube input, file drops, AI analysis, UI events, and exports
 */
document.addEventListener('DOMContentLoaded', () => {
  'use strict';

  // State
  let savedUser = null;
  try {
    savedUser = JSON.parse(localStorage.getItem('viralclip_user') || 'null');
  } catch(e) {
    savedUser = null;
  }

  const state = {
    currentVideo: null,
    targetDuration: 30,
    apiKey: localStorage.getItem('viralclip_gemini_key') || '',
    apiBase: localStorage.getItem('viralclip_api_base') || 'api',
    isExporting: false,
    user: savedUser
  };

  // UI Elements
  const ytForm = document.getElementById('ytForm');
  const ytUrlInput = document.getElementById('ytUrlInput');
  const clearUrlBtn = document.getElementById('clearUrlBtn');
  const emptyState = document.getElementById('emptyState');
  const videoLoader = document.getElementById('videoLoader');
  const loaderMessage = document.getElementById('loaderMessage');
  const currentVideoTitle = document.getElementById('currentVideoTitle');
  const currentVideoDuration = document.getElementById('currentVideoDuration');
  const suggestionsContainer = document.getElementById('suggestionsContainer');
  const aiPremiseText = document.getElementById('aiPremiseText');

  // Hook customization elements
  const hookTextInput = document.getElementById('hookTextInput');
  const enableHookOverlay = document.getElementById('enableHookOverlay');

  // Initialize Video Engine
  VideoEngine.init({
    videoElementId: 'sourceVideo',
    canvasId: 'renderCanvas'
  });

  // Check stored settings
  const storedKey = localStorage.getItem('viralclip_gemini_key') || '';
  if (storedKey) {
    const keyInput = document.getElementById('geminiApiKeyInput');
    if (keyInput) keyInput.value = storedKey;
  }
  const storedBase = localStorage.getItem('viralclip_api_base');
  if (storedBase) {
    const baseInput = document.getElementById('apiBaseUrlInput');
    if (baseInput) baseInput.value = storedBase;
  }
  const storedColab = (localStorage.getItem('viralclip_colab_url') || '').replace(/\/+$/, '');
  state.colabUrl = storedColab;
  const colabInput = document.getElementById('colabWorkerUrlInput');
  if (colabInput && storedColab) {
    colabInput.value = storedColab;
  }

  // ========================================================
  // Tabs Navigation
  // ========================================================
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const targetTab = document.getElementById(btn.dataset.tab);
      if (targetTab) targetTab.classList.add('active');
    });
  });

  // Clear URL button
  clearUrlBtn.addEventListener('click', () => {
    ytUrlInput.value = '';
    ytUrlInput.focus();
  });

  // ========================================================
  // YouTube Form Submission & AI Analysis
  // ========================================================
  let isFetchingYouTube = false;
  async function handleYouTubeFetch() {
    if (isFetchingYouTube) return;
    const url = ytUrlInput.value.trim();
    if (!url) return;

    if (!YouTubeHelper.isValidYouTubeUrl(url)) {
      showToast('Please enter a valid YouTube or YouTube Shorts URL', 'warning');
      return;
    }

    try {
      isFetchingYouTube = true;
      showLoader('Fetching video details & analyzing viral hooks...');
      
      // 1. Fetch metadata
      const meta = await YouTubeHelper.fetchMetadata(url, state.apiBase);
      state.currentVideo = meta;

      currentVideoTitle.textContent = meta.title;
      currentVideoDuration.textContent = formatDuration(meta.duration);

      // Load the YouTube Video directly into the studio player immediately
      await VideoEngine.loadYouTubeVideo(meta.id, meta.duration);
      emptyState.style.display = 'none';

      // 2. Fetch AI viral suggestions
      loaderMessage.textContent = 'Analyzing virality & retention curve with Gemini AI...';
      const aiResults = await AIAnalyzer.getViralSuggestions({
        title: meta.title,
        description: meta.description,
        duration: meta.duration,
        targetDuration: state.targetDuration,
        apiKey: state.apiKey,
        apiBase: state.apiBase
      });

      if (aiResults && aiResults.premise) {
        aiPremiseText.textContent = aiResults.premise;
      }

      // Render suggestions in sidebar
      AIAnalyzer.renderSuggestions(aiResults, suggestionsContainer, (clip) => {
        applyClipToStudio(clip);
      });

      showToast('YouTube video loaded & analyzed with AI hooks!', 'success');

      // Automatically apply the #1 viral hook segment
      if (aiResults && aiResults.clips && aiResults.clips.length > 0) {
        applyClipToStudio(aiResults.clips[0]);
      }

    } catch (err) {
      console.error(err);
      showToast(err.message || 'Error processing YouTube video.', 'error');
    } finally {
      isFetchingYouTube = false;
      hideLoader();
    }
  }

  ytForm.addEventListener('submit', (e) => {
    e.preventDefault();
    handleYouTubeFetch();
  });

  if (fetchYtBtn) {
    fetchYtBtn.addEventListener('click', (e) => {
      e.preventDefault();
      handleYouTubeFetch();
    });
  }

  // Re-Analyze button in AI suggestions panel
  document.getElementById('reanalyzeBtn').addEventListener('click', async () => {
    if (!state.currentVideo) {
      showToast('Load a video first to analyze viral hooks.', 'warning');
      return;
    }
    try {
      showLoader('Re-analyzing viral hooks with Gemini Flash...');
      const aiResults = await AIAnalyzer.getViralSuggestions({
        title: state.currentVideo.title,
        description: state.currentVideo.description,
        duration: VideoEngine.getTotalDuration(),
        targetDuration: state.targetDuration,
        apiKey: state.apiKey,
        apiBase: state.apiBase
      });

      if (aiResults && aiResults.premise) {
        aiPremiseText.textContent = aiResults.premise;
      }

      AIAnalyzer.renderSuggestions(aiResults, suggestionsContainer, (clip) => {
        applyClipToStudio(clip);
      });

      showToast('AI suggestions updated!', 'success');
    } catch (e) {
      showToast('Failed to re-analyze.', 'error');
    } finally {
      hideLoader();
    }
  });

  // ========================================================
  // Local File Upload & Drag and Drop
  // ========================================================
  const dropzone = document.getElementById('videoDropzone');
  const fileInput = document.getElementById('localFileInput');

  dropzone.addEventListener('click', () => fileInput.click());

  dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
  });

  dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));

  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleLocalVideoFile(e.dataTransfer.files[0]);
    }
  });

  fileInput.addEventListener('change', (e) => {
    if (e.target.files && e.target.files[0]) {
      handleLocalVideoFile(e.target.files[0]);
    }
  });

  async function handleLocalVideoFile(file) {
    try {
      showLoader('Loading local video into memory...');
      await VideoEngine.loadFile(file);
      emptyState.style.display = 'none';

      const totalDur = VideoEngine.getTotalDuration();
      state.currentVideo = {
        title: file.name.replace(/\.[^/.]+$/, ''),
        description: 'Locally uploaded video file',
        duration: totalDur
      };

      currentVideoTitle.textContent = state.currentVideo.title;
      currentVideoDuration.textContent = formatDuration(totalDur);

      loaderMessage.textContent = 'Generating AI retention suggestions...';
      const aiResults = await AIAnalyzer.getViralSuggestions({
        title: state.currentVideo.title,
        description: '',
        duration: totalDur,
        targetDuration: state.targetDuration,
        apiKey: state.apiKey,
        apiBase: state.apiBase
      });

      AIAnalyzer.renderSuggestions(aiResults, suggestionsContainer, (clip) => {
        applyClipToStudio(clip);
      });

      if (aiResults && aiResults.clips && aiResults.clips.length > 0) {
        applyClipToStudio(aiResults.clips[0]);
      }

      showToast(`Loaded "${file.name}" successfully!`, 'success');
    } catch (err) {
      showToast('Failed to load video file.', 'error');
    } finally {
      hideLoader();
    }
  }

  // ========================================================
  // Demo Samples
  // ========================================================
  const sampleVideos = {
    nature: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    tech: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    podcast: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4'
  };

  document.querySelectorAll('.sample-card').forEach(btn => {
    btn.addEventListener('click', () => {
      const sampleKey = btn.dataset.sample;
      loadDemoVideo(sampleKey);
    });
  });

  async function loadDemoVideo(sampleKey) {
    const url = sampleVideos[sampleKey] || sampleVideos.tech;
    try {
      showLoader('Loading demo video...');
      await VideoEngine.loadSource(url);
      emptyState.style.display = 'none';

      const sampleNames = {
        nature: 'Scenic 4K Drone Mountain Footage',
        tech: 'Animated Feature Film Story',
        podcast: 'Inspiring Creator Interview & Talk'
      };

      const title = sampleNames[sampleKey] || 'Sample Creator Video';
      const totalDur = VideoEngine.getTotalDuration();

      state.currentVideo = {
        title: title,
        description: 'High definition creator sample footage',
        duration: totalDur
      };

      currentVideoTitle.textContent = title;
      currentVideoDuration.textContent = formatDuration(totalDur);

      const aiResults = await AIAnalyzer.getViralSuggestions({
        title: title,
        description: '',
        duration: totalDur,
        targetDuration: state.targetDuration,
        apiKey: state.apiKey,
        apiBase: state.apiBase
      });

      AIAnalyzer.renderSuggestions(aiResults, suggestionsContainer, (clip) => {
        applyClipToStudio(clip);
      });

      if (aiResults && aiResults.clips && aiResults.clips.length > 0) {
        applyClipToStudio(aiResults.clips[0]);
      }

      showToast('Demo video loaded ready for editing!', 'success');
    } catch (e) {
      showToast('Could not load demo video.', 'error');
    } finally {
      hideLoader();
    }
  }

  // ========================================================
  // Applying an AI Clip to Timeline & Hook
  // ========================================================
  function applyClipToStudio(clip) {
    if (!clip) return;
    state.currentClip = clip;
    
    // Set segment timestamps
    VideoEngine.setClipSegment(clip.start_seconds, clip.end_seconds);

    // Apply overlay hook text
    if (clip.caption_overlay) {
      hookTextInput.value = clip.caption_overlay;
      VideoEngine.setHookOverlay({ text: clip.caption_overlay });
    }

    showToast(`Applied clip: ${clip.title}`, 'info');
  }

  // ========================================================
  // Playback & Trimmer Controls
  // ========================================================
  document.getElementById('playPauseBtn').addEventListener('click', () => {
    VideoEngine.togglePlay();
  });

  document.getElementById('restartClipBtn').addEventListener('click', () => {
    VideoEngine.replayClip();
  });

  document.getElementById('muteBtn').addEventListener('click', () => {
    VideoEngine.toggleMute();
  });

  // Duration Presets (15s, 30s, 45s, 60s)
  document.querySelectorAll('.preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.preset-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const dur = parseInt(btn.dataset.dur, 10);
      state.targetDuration = dur;
      VideoEngine.setClipDuration(dur);
    });
  });

  // Custom duration input
  document.getElementById('applyCustomDurBtn').addEventListener('click', () => {
    const val = parseInt(document.getElementById('customSecInput').value, 10);
    if (val && val >= 5) {
      state.targetDuration = val;
      document.querySelectorAll('.preset-btn').forEach(b => b.classList.remove('active'));
      VideoEngine.setClipDuration(val);
      showToast(`Set duration to ${val}s`, 'info');
    }
  });

  // Aspect Ratio buttons
  document.querySelectorAll('.aspect-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.aspect-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      VideoEngine.setAspect(btn.dataset.aspect);
    });
  });

  // Framing buttons (Center, Left, Right, Fit)
  document.getElementById('cropCenterBtn').addEventListener('click', (e) => {
    setActiveFramingBtn(e.currentTarget);
    VideoEngine.setCropMode('center');
  });
  document.getElementById('cropLeftBtn').addEventListener('click', (e) => {
    setActiveFramingBtn(e.currentTarget);
    VideoEngine.setCropMode('left');
  });
  document.getElementById('cropRightBtn').addEventListener('click', (e) => {
    setActiveFramingBtn(e.currentTarget);
    VideoEngine.setCropMode('right');
  });
  document.getElementById('cropFitBtn').addEventListener('click', (e) => {
    setActiveFramingBtn(e.currentTarget);
    VideoEngine.setCropMode('fit');
  });

  function setActiveFramingBtn(activeBtn) {
    document.querySelectorAll('.framing-toolbar .btn-tool').forEach(b => b.classList.remove('active'));
    activeBtn.classList.add('active');
  }

  // Hook Overlay customizer inputs
  hookTextInput.addEventListener('input', (e) => {
    VideoEngine.setHookOverlay({ text: e.target.value });
  });

  enableHookOverlay.addEventListener('change', (e) => {
    VideoEngine.setHookOverlay({ enabled: e.target.checked });
  });

  document.querySelectorAll('.style-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      document.querySelectorAll('.style-pill').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      VideoEngine.setHookOverlay({ style: pill.dataset.style });
    });
  });

  document.querySelectorAll('.pos-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.pos-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      VideoEngine.setHookOverlay({ position: btn.dataset.pos });
    });
  });

  // ========================================================
  // User Authentication & Download Gate
  // ========================================================
  const authModal = document.getElementById('authModal');
  const openAuthBtn = document.getElementById('openAuthBtn');
  const closeAuthBtn = document.getElementById('closeAuthBtn');
  const userBadge = document.getElementById('userBadge');
  const userNameDisplay = document.getElementById('userNameDisplay');
  const logoutBtn = document.getElementById('logoutBtn');
  const tabLoginBtn = document.getElementById('tabLoginBtn');
  const tabRegisterBtn = document.getElementById('tabRegisterBtn');
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const quickDemoLoginBtn = document.getElementById('quickDemoLoginBtn');
  const exportBtn = document.getElementById('recordExportBtn');
  const progressContainer = document.getElementById('exportProgressContainer');
  const progressBar = document.getElementById('exportProgressBar');
  const progressText = document.getElementById('exportProgressText');

  const adminPanelLink = document.getElementById('adminPanelLink');
  const quickAdminLoginBtn = document.getElementById('quickAdminLoginBtn');

  function updateAuthUI() {
    if (state.user) {
      if (openAuthBtn) openAuthBtn.style.display = 'none';
      if (userBadge) {
        userBadge.style.display = 'flex';
        userNameDisplay.textContent = state.user.name || state.user.email.split('@')[0];
      }
      if (adminPanelLink) {
        adminPanelLink.style.display = (state.user.role === 'admin' || state.user.email === 'pallanti2020@gmail.com') ? 'inline-flex' : 'none';
      }
      exportBtn.innerHTML = '<i class="fa-solid fa-download"></i> <span>Export & Download Short</span>';
    } else {
      if (openAuthBtn) openAuthBtn.style.display = 'inline-flex';
      if (userBadge) userBadge.style.display = 'none';
      if (adminPanelLink) adminPanelLink.style.display = 'none';
      exportBtn.innerHTML = '<i class="fa-solid fa-lock"></i> <span>Export Short (Sign In Required)</span>';
    }
  }
  updateAuthUI();

  if (openAuthBtn) {
    openAuthBtn.addEventListener('click', () => {
      authModal.style.display = 'flex';
    });
  }
  if (closeAuthBtn) {
    closeAuthBtn.addEventListener('click', () => {
      authModal.style.display = 'none';
    });
  }

  // Auth Tabs (Login Only - Public Registration Disabled)
  if (tabLoginBtn) {
    tabLoginBtn.addEventListener('click', () => {
      tabLoginBtn.classList.add('active');
      if (tabRegisterBtn) tabRegisterBtn.classList.remove('active');
      if (loginForm) loginForm.style.display = 'flex';
      if (registerForm) registerForm.style.display = 'none';
    });
  }

  // Login Submit
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('loginEmail').value.trim();
      const password = document.getElementById('loginPassword').value;

      try {
        showLoader('Signing in to creator portal...');

        // Direct check for Super Admin
        if (email.toLowerCase() === 'pallanti2020@gmail.com' && password === 'Abhi@9246830084') {
          const adminUser = {
            id: 1,
            name: 'Pallanti Admin',
            email: 'pallanti2020@gmail.com',
            role: 'admin',
            status: 'approved'
          };
          state.user = adminUser;
          localStorage.setItem('viralclip_user', JSON.stringify(adminUser));
          localStorage.setItem('viralclip_admin_user', JSON.stringify(adminUser));
          localStorage.setItem('viralclip_admin_token', btoa('1:pallanti2020@gmail.com'));
          localStorage.setItem('viralclip_token', btoa('1:pallanti2020@gmail.com'));
          updateAuthUI();
          authModal.style.display = 'none';
          showToast('Welcome Administrator! Admin Panel unlocked in header.', 'success');
          return;
        }

        let user = null;
        let authToken = '';
        try {
          const getUrl = `${state.apiBase}/auth.php?action=login&email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`;
          const res = await fetch(getUrl);
          const data = await res.json();

          if (!res.ok || data.error) {
            throw new Error(data.error || 'Login failed.');
          }
          if (data && data.user) {
            user = data.user;
            authToken = data.token || '';
          }
        } catch (netErr) {
          if (netErr.message && (netErr.message.includes('pending') || netErr.message.includes('password') || netErr.message.includes('Account not found'))) {
            throw netErr;
          }
          console.warn('Backend auth unreachable:', netErr);
        }

        if (!user) {
          throw new Error('Access Denied: Only admin-approved accounts can access the portal.');
        }

        state.user = user;
        localStorage.setItem('viralclip_user', JSON.stringify(user));
        if (authToken) {
          localStorage.setItem('viralclip_token', authToken);
        }
        updateAuthUI();
        authModal.style.display = 'none';
        showToast(`Welcome back, ${user.name}! You can now use the studio.`, 'success');
      } catch (err) {
        showToast(err.message, 'error');
      } finally {
        hideLoader();
      }
    });
  }

  // Register form disabled (Admin creates accounts directly)
  if (registerForm) {
    registerForm.addEventListener('submit', (e) => {
      e.preventDefault();
      showToast('Public registration is disabled. Please contact the administrator (pallanti2020@gmail.com) to create your account.', 'warning');
    });
  }

  // 1-Click Admin Login Shortcut
  if (quickAdminLoginBtn) {
    quickAdminLoginBtn.addEventListener('click', () => {
      document.getElementById('loginEmail').value = 'pallanti2020@gmail.com';
      document.getElementById('loginPassword').value = 'Abhi@9246830084';
      loginForm.dispatchEvent(new Event('submit'));
    });
  }

  // Logout
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      state.user = null;
      localStorage.removeItem('viralclip_user');
      localStorage.removeItem('viralclip_token');
      localStorage.removeItem('viralclip_admin_user');
      localStorage.removeItem('viralclip_admin_token');
      updateAuthUI();
      showToast('You have signed out.', 'info');
    });
  }

  // ========================================================
  // Export & Download (Protected by Auth!)
  // ========================================================
  function openYouTubeExportModal() {
    const ytModal = document.getElementById('ytExportModal');
    if (!ytModal) return;

    const video = state.currentVideo || {};
    const videoId = video.id || (video.url ? YouTubeHelper.extractVideoId(video.url) : '');
    const startTimeSec = Math.round(VideoEngine.getStartTime());
    const endTimeSec = Math.round(VideoEngine.getEndTime());
    const durationSec = Math.max(1, endTimeSec - startTimeSec);
    const startFormatted = formatDuration(startTimeSec);
    const endFormatted = formatDuration(endTimeSec);

    // Populate video info
    const thumbEl = document.getElementById('ytExportThumb');
    if (thumbEl) thumbEl.src = video.thumbnail || (videoId ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : '');
    const titleEl = document.getElementById('ytExportTitle');
    if (titleEl) titleEl.textContent = video.title || 'YouTube Short';
    const timeEl = document.getElementById('ytExportTimeRange');
    if (timeEl) timeEl.innerHTML = `<i class="fa-solid fa-scissors"></i> Clip: ${startFormatted} - ${endFormatted} (${durationSec}s)`;

    // Configure 1-Click direct MP4 download links
    const directBtn = document.getElementById('directMp4DownloadBtn');
    if (directBtn) {
      directBtn.href = `https://10downloader.com/download?v=${videoId}&type=video`;
    }
    const backupBtn1 = document.getElementById('backupDownloadBtn1');
    if (backupBtn1) {
      backupBtn1.href = `https://ssyoutube.com/watch?v=${videoId}`;
    }
    const backupBtn2 = document.getElementById('backupDownloadBtn2');
    if (backupBtn2) {
      backupBtn2.href = `https://y2mate.is/watch?v=${videoId}`;
    }

    // Populate copyable viral hook and timestamps
    const hookOverlay = (hookTextInput ? hookTextInput.value : '') || state.currentClip?.caption_overlay || 'WAIT TILL THE END... 🤯';
    const clipTitle = state.currentClip?.title || video.title || 'Viral Short';
    const hookReason = state.currentClip?.hook_reason || 'High retention opening hook';
    const tags = (state.currentClip?.hashtags && state.currentClip.hashtags.length > 0)
      ? state.currentClip.hashtags.join(' ')
      : '#Shorts #Viral #Reels #TikTok #Trending';

    const copyText = `🔥 Title: ${clipTitle}\n⏱️ Timestamps: ${startFormatted} - ${endFormatted} (${durationSec}s)\n🎯 Hook: ${hookReason}\n💬 Caption: ${hookOverlay}\n🏷️ Tags: ${tags}`;

    const textarea = document.getElementById('viralHookTextarea');
    if (textarea) textarea.value = copyText;

    // Check Colab connection state
    const colabBadge = document.getElementById('colabStatusBadge');
    if (colabBadge) {
      if (state.colabUrl) {
        colabBadge.textContent = 'Colab Connected 🟢';
        colabBadge.style.color = '#34d399';
        colabBadge.style.background = 'rgba(16, 185, 129, 0.18)';
      } else {
        colabBadge.textContent = '1-Click Direct Download';
        colabBadge.style.color = '#c084fc';
        colabBadge.style.background = 'rgba(168, 85, 247, 0.18)';
      }
    }

    ytModal.style.display = 'flex';
  }

  // Colab 1-Click Render Controls
  const configColabBtn = document.getElementById('configColabBtn');
  if (configColabBtn) {
    configColabBtn.addEventListener('click', () => {
      document.getElementById('settingsModal').style.display = 'flex';
      setTimeout(() => {
        const inp = document.getElementById('colabWorkerUrlInput');
        if (inp) inp.focus();
      }, 200);
    });
  }

  const startColabRenderBtn = document.getElementById('startColabRenderBtn');
  if (startColabRenderBtn) {
    startColabRenderBtn.addEventListener('click', async () => {
      let colabBase = (state.colabUrl || localStorage.getItem('viralclip_colab_url') || '').trim().replace(/\/+$/, '');
      if (!colabBase) {
        const entered = prompt('Please enter your Google Colab Cloudflare URL (e.g. https://xxxx.trycloudflare.com):\n\nIf you have not started the notebook yet, open Google Colab first to run the 12GB worker.', '');
        if (entered && entered.trim()) {
          colabBase = entered.trim().replace(/\/+$/, '');
          state.colabUrl = colabBase;
          localStorage.setItem('viralclip_colab_url', colabBase);
          const inp = document.getElementById('colabWorkerUrlInput');
          if (inp) inp.value = colabBase;
          const colabBadge = document.getElementById('colabStatusBadge');
          if (colabBadge) {
            colabBadge.textContent = 'Colab Connected 🟢';
            colabBadge.style.color = '#34d399';
            colabBadge.style.background = 'rgba(16, 185, 129, 0.18)';
          }
        } else {
          showToast('Please enter your active Colab URL to render directly!', 'warning');
          return;
        }
      }

      const progressEl = document.getElementById('colabRenderProgress');
      const progressText = document.getElementById('colabProgressText');

      try {
        startColabRenderBtn.disabled = true;
        if (progressEl) progressEl.style.display = 'block';
        if (progressText) progressText.textContent = 'Connecting to Google Colab 12GB Worker...';

        const startTime = VideoEngine.getStartTime();
        const endTime = VideoEngine.getEndTime();
        const hookText = (document.getElementById('hookTextInput') ? document.getElementById('hookTextInput').value : '') || '';

        const payload = {
          url: state.currentVideo ? state.currentVideo.url : '',
          start_seconds: startTime,
          end_seconds: endTime,
          hook_text: hookText
        };

        if (progressText) progressText.textContent = 'Downloading segment & cropping 9:16 with FFmpeg (12GB RAM)...';

        const res = await fetch(`${colabBase}/api/render`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        if (!res.ok) {
          const errData = await res.json().catch(() => ({}));
          throw new Error(errData.detail || `Colab returned HTTP ${res.status}`);
        }

        const data = await res.json();
        if (data && data.download_url) {
          const fullDownloadUrl = `${colabBase}${data.download_url}`;
          if (progressText) progressText.textContent = 'Render complete! Starting file download...';

          const a = document.createElement('a');
          a.href = fullDownloadUrl;
          a.download = `short_${data.job_id || 'clip'}_916.mp4`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);

          showToast('1-Click Short Rendered & Downloaded Successfully! 🎉', 'success');
        } else {
          throw new Error('Colab worker did not return a valid download URL.');
        }

      } catch (err) {
        console.error('Colab render error:', err);
        showToast(`Colab error: ${err.message}`, 'error');
      } finally {
        startColabRenderBtn.disabled = false;
        if (progressEl) {
          setTimeout(() => {
            progressEl.style.display = 'none';
          }, 4000);
        }
      }
    });
  }

  const copyViralHookBtn = document.getElementById('copyViralHookBtn');
  if (copyViralHookBtn) {
    copyViralHookBtn.addEventListener('click', async () => {
      const textarea = document.getElementById('viralHookTextarea');
      const text = textarea ? textarea.value : '';
      if (!text) return;
      try {
        await navigator.clipboard.writeText(text);
        const btnText = document.getElementById('copyViralHookBtnText');
        if (btnText) btnText.textContent = '✓ Copied to Clipboard!';
        showToast('Timestamps & viral hook copied to clipboard!', 'success');
        setTimeout(() => {
          if (btnText) btnText.textContent = 'Copy Timestamps & Viral Hook';
        }, 3000);
      } catch (err) {
        textarea.select();
        document.execCommand('copy');
        showToast('Copied to clipboard!', 'success');
      }
    });
  }

  exportBtn.addEventListener('click', () => {
    // Check if user is logged in
    if (!state.user) {
      authModal.style.display = 'flex';
      showToast('Please sign in or create a free account to export your video!', 'warning');
      return;
    }

    if (VideoEngine.getActiveMode() === 'youtube') {
      openYouTubeExportModal();
      return;
    }

    if (state.isExporting) return;
    state.isExporting = true;
    exportBtn.disabled = true;
    progressContainer.style.display = 'flex';
    progressBar.style.width = '0%';
    progressText.textContent = 'Rendering Short: 0%';

    VideoEngine.exportClip({
      onProgress: (pct) => {
        progressBar.style.width = `${pct}%`;
        progressText.textContent = `Rendering Short: ${pct}%`;
      },
      onComplete: ({ fileName }) => {
        state.isExporting = false;
        exportBtn.disabled = false;
        progressBar.style.width = '100%';
        progressText.textContent = 'Rendering Complete!';
        setTimeout(() => {
          progressContainer.style.display = 'none';
        }, 3000);
        showToast(`Short exported as ${fileName}! Download starting...`, 'success');
      },
      onError: (errMsg) => {
        state.isExporting = false;
        exportBtn.disabled = false;
        progressContainer.style.display = 'none';
        showToast(errMsg, 'error');
      }
    });
  });

  // ========================================================
  // Modals & Settings
  // ========================================================
  const settingsModal = document.getElementById('settingsModal');

  document.getElementById('openSettingsBtn').addEventListener('click', () => {
    settingsModal.style.display = 'flex';
  });
  document.getElementById('closeSettingsBtn').addEventListener('click', () => {
    settingsModal.style.display = 'none';
  });
  document.getElementById('saveSettingsBtn').addEventListener('click', () => {
    const keyInput = document.getElementById('geminiApiKeyInput');
    const key = keyInput ? keyInput.value.trim() : (state.apiKey || '');
    const baseInput = document.getElementById('apiBaseUrlInput');
    const base = baseInput ? baseInput.value.trim() : (state.apiBase || 'api');
    const colabInput = document.getElementById('colabWorkerUrlInput');
    const colabUrl = colabInput ? colabInput.value.trim().replace(/\/+$/, '') : '';
    state.apiKey = key;
    state.apiBase = base;
    state.colabUrl = colabUrl;
    localStorage.setItem('viralclip_gemini_key', key);
    localStorage.setItem('viralclip_api_base', base);
    localStorage.setItem('viralclip_colab_url', colabUrl);
    settingsModal.style.display = 'none';
    showToast('Settings saved successfully!', 'success');
  });

  // Utility helpers
  function showLoader(msg) {
    loaderMessage.textContent = msg || 'Loading...';
    videoLoader.style.display = 'flex';
  }

  function hideLoader() {
    videoLoader.style.display = 'none';
  }

  function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    const toastMsg = toast.querySelector('.toast-message');
    const toastIcon = toast.querySelector('.toast-icon');

    toastMsg.textContent = message;
    if (type === 'error' || type === 'warning') {
      toastIcon.className = 'toast-icon fa-solid fa-triangle-exclamation';
      toastIcon.style.color = 'var(--danger)';
    } else {
      toastIcon.className = 'toast-icon fa-solid fa-circle-check';
      toastIcon.style.color = 'var(--success)';
    }

    toast.style.display = 'flex';
    clearTimeout(toast._timeout);
    toast._timeout = setTimeout(() => {
      toast.style.display = 'none';
    }, 4000);
  }
  window.showToast = showToast;

  function formatDuration(sec) {
    const s = Math.floor(sec || 0);
    const mins = Math.floor(s / 60);
    const secs = s % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }
});
