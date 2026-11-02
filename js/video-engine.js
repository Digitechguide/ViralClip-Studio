/**
 * Video Engine Module
 * High-performance canvas compositor, dual-mode YouTube Player & Canvas Engine,
 * aspect ratio cropper, interactive timeline trimmer, hook overlay renderer, and exporter.
 */
const VideoEngine = (function() {
  'use strict';

  let videoEl = null;
  let canvas = null;
  let ctx = null;
  let animFrameId = null;

  // Mode: 'native' (canvas/file) or 'youtube' (IFrame API)
  let activeMode = 'native';
  let ytPlayer = null;
  let ytTicker = null;
  let currentYtVideoId = null;

  // Trimming State
  let startTime = 0;
  let endTime = 30;
  let totalDuration = 0;
  let isPlaying = false;
  let isDragging = null; // 'start', 'end', 'playhead'

  // Framing & Appearance
  let aspectMode = '9:16'; // '9:16', '1:1', '16:9'
  let cropMode = 'center'; // 'center', 'left', 'right', 'fit'
  let hookText = 'WAIT FOR THIS PART... 🤯';
  let hookStyle = 'yellow-black';
  let hookPosition = 'top';
  let showHookOverlay = true;

  // DOM Elements
  let timelineTrack = null;
  let segmentHighlight = null;
  let timelinePlayhead = null;
  let startHandle = null;
  let endHandle = null;
  let startTimeLabel = null;
  let endTimeLabel = null;
  let currentTimeDisplay = null;
  let totalDurationDisplay = null;
  let selectedClipLength = null;
  let ytWrapper = null;
  let liveHookBanner = null;

  function init({
    videoElementId = 'sourceVideo',
    canvasId = 'renderCanvas',
    onTimeUpdateCallback = null
  }) {
    videoEl = document.getElementById(videoElementId);
    canvas = document.getElementById(canvasId);
    ctx = canvas.getContext('2d');

    ytWrapper = document.getElementById('ytPlayerWrapper');
    liveHookBanner = document.getElementById('liveHookBanner');

    // Timeline elements
    timelineTrack = document.getElementById('timelineTrack');
    segmentHighlight = document.getElementById('segmentHighlight');
    timelinePlayhead = document.getElementById('timelinePlayhead');
    startHandle = document.getElementById('startHandle');
    endHandle = document.getElementById('endHandle');
    startTimeLabel = document.getElementById('startTimeLabel');
    endTimeLabel = document.getElementById('endTimeLabel');
    currentTimeDisplay = document.getElementById('currentTimeDisplay');
    totalDurationDisplay = document.getElementById('totalDurationDisplay');
    selectedClipLength = document.getElementById('selectedClipLength');

    setupVideoEvents(onTimeUpdateCallback);
    setupTimelineEvents();
    startCanvasLoop();
  }

  function setupVideoEvents(onTimeUpdateCallback) {
    videoEl.addEventListener('loadedmetadata', () => {
      if (activeMode === 'native') {
        totalDuration = videoEl.duration || 60;
        startTime = 0;
        endTime = Math.min(30, totalDuration);
        updateTimelineUI();
        updateTimeDisplays();
        setCanvasResolution();
      }
    });

    videoEl.addEventListener('timeupdate', () => {
      if (activeMode !== 'native') return;

      // Loop within the selected trim segment
      if (videoEl.currentTime >= endTime) {
        videoEl.currentTime = startTime;
        if (isPlaying) videoEl.play();
      } else if (videoEl.currentTime < startTime) {
        videoEl.currentTime = startTime;
      }

      updatePlayheadPosition(videoEl.currentTime);
      updateTimeDisplays(videoEl.currentTime);

      if (typeof onTimeUpdateCallback === 'function') {
        onTimeUpdateCallback(videoEl.currentTime);
      }
    });

    videoEl.addEventListener('play', () => {
      isPlaying = true;
      setPlayButtonIcon(true);
    });

    videoEl.addEventListener('pause', () => {
      isPlaying = false;
      setPlayButtonIcon(false);
    });
  }

  function setPlayButtonIcon(playing) {
    const playIcon = document.querySelector('#playPauseBtn i');
    if (playIcon) {
      playIcon.className = playing ? 'fa-solid fa-pause' : 'fa-solid fa-play';
    }
  }

  function setCanvasResolution() {
    if (aspectMode === '9:16') {
      canvas.width = 720;
      canvas.height = 1280;
    } else if (aspectMode === '1:1') {
      canvas.width = 720;
      canvas.height = 720;
    } else {
      canvas.width = 1280;
      canvas.height = 720;
    }
  }

  // ========================================================
  // YouTube Video Loader (Real YouTube Player)
  // ========================================================
  function loadYouTubeVideo(videoId, metaDuration = 180) {
    return new Promise((resolve) => {
      activeMode = 'youtube';
      currentYtVideoId = videoId;
      totalDuration = metaDuration || 180;
      startTime = 0;
      endTime = Math.min(30, totalDuration);

      // Stop native video safely
      if (videoEl && typeof videoEl.pause === 'function') {
        try {
          videoEl.pause();
          videoEl.src = '';
        } catch(e) {}
      }

      // Switch views: hide canvas, show real YouTube iframe and floating live hook
      if (canvas) canvas.style.display = 'none';
      if (ytWrapper) ytWrapper.style.display = 'block';
      if (liveHookBanner) {
        liveHookBanner.style.display = showHookOverlay ? 'block' : 'none';
      }

      const iframe = document.getElementById('ytIframe');
      if (iframe) {
        // Embed real YouTube video directly into the smartphone mockup
        iframe.src = `https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&enablejsapi=1&rel=0&modestbranding=1&playsinline=1&start=${Math.floor(startTime)}`;
      }

      updateTimelineUI();
      updateTimeDisplays(0);
      resolve();
    });
  }

  function startYouTubeTicker() {
    clearInterval(ytTicker);
    ytTicker = setInterval(() => {
      if (activeMode !== 'youtube') return;
      // Simulated playhead progress during playback if playing
      if (isPlaying) {
        let curr = (parseFloat(currentTimeDisplay.textContent.split(':')[0]) * 60) + parseFloat(currentTimeDisplay.textContent.split(':')[1]) + 0.25;
        if (curr >= endTime) {
          curr = startTime;
          seekTo(startTime);
        }
        updatePlayheadPosition(curr);
        updateTimeDisplays(curr);
      }
    }, 250);
  }

  function loadSource(srcUrl) {
    return new Promise((resolve, reject) => {
      activeMode = 'native';
      clearInterval(ytTicker);
      if (ytPlayer && typeof ytPlayer.pauseVideo === 'function') {
        ytPlayer.pauseVideo();
      }

      if (canvas) canvas.style.display = 'block';
      if (ytWrapper) ytWrapper.style.display = 'none';
      if (liveHookBanner) liveHookBanner.style.display = 'none';

      videoEl.src = srcUrl;
      videoEl.load();
      videoEl.oncanplay = () => resolve();
      videoEl.onerror = (e) => reject(new Error('Unable to play video source.'));
    });
  }

  function loadFile(file) {
    return new Promise((resolve, reject) => {
      activeMode = 'native';
      clearInterval(ytTicker);
      if (ytPlayer && typeof ytPlayer.pauseVideo === 'function') {
        ytPlayer.pauseVideo();
      }

      if (canvas) canvas.style.display = 'block';
      if (ytWrapper) ytWrapper.style.display = 'none';
      if (liveHookBanner) liveHookBanner.style.display = 'none';

      const blobUrl = URL.createObjectURL(file);
      videoEl.src = blobUrl;
      videoEl.load();
      videoEl.oncanplay = () => resolve();
      videoEl.onerror = (e) => reject(new Error('Unable to read local video file.'));
    });
  }

  function startCanvasLoop() {
    function draw() {
      if (activeMode === 'native') {
        renderFrame();
      }
      animFrameId = requestAnimationFrame(draw);
    }
    cancelAnimationFrame(animFrameId);
    animFrameId = requestAnimationFrame(draw);
  }

  function renderFrame() {
    if (!ctx || !canvas) return;
    const cw = canvas.width;
    const ch = canvas.height;

    ctx.fillStyle = '#0a0c13';
    ctx.fillRect(0, 0, cw, ch);

    if (videoEl && videoEl.readyState >= 2) {
      const vw = videoEl.videoWidth || 1920;
      const vh = videoEl.videoHeight || 1080;

      if (cropMode === 'fit') {
        ctx.save();
        ctx.filter = 'blur(20px) brightness(0.6)';
        ctx.drawImage(videoEl, -cw * 0.1, -ch * 0.1, cw * 1.2, ch * 1.2);
        ctx.restore();

        const scale = Math.min(cw / vw, ch / vh);
        const dw = vw * scale;
        const dh = vh * scale;
        const dx = (cw - dw) / 2;
        const dy = (ch - dh) / 2;
        ctx.drawImage(videoEl, dx, dy, dw, dh);
      } else {
        const targetRatio = cw / ch;
        const sourceRatio = vw / vh;
        let sx, sy, sw, sh;

        if (sourceRatio > targetRatio) {
          sh = vh;
          sw = vh * targetRatio;
          sy = 0;
          if (cropMode === 'left') sx = 0;
          else if (cropMode === 'right') sx = vw - sw;
          else sx = (vw - sw) / 2;
        } else {
          sw = vw;
          sh = vw / targetRatio;
          sx = 0;
          sy = (vh - sh) / 2;
        }

        ctx.drawImage(videoEl, sx, sy, sw, sh, 0, 0, cw, ch);
      }

      if (showHookOverlay && hookText && hookText.trim().length > 0) {
        drawHookOverlay(ctx, cw, ch);
      }
    }
  }

  function drawHookOverlay(ctx, cw, ch) {
    ctx.save();
    const text = hookText.trim().toUpperCase();
    const fontSize = Math.round(cw * 0.058);
    ctx.font = `800 ${fontSize}px 'Plus Jakarta Sans', Impact, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';

    let textY = ch * 0.16;
    if (hookPosition === 'middle') textY = ch * 0.50;
    else if (hookPosition === 'bottom') textY = ch * 0.82;

    const words = text.split(' ');
    let lines = [];
    let currentLine = words[0] || '';
    const maxLineWidth = cw * 0.84;

    for (let i = 1; i < words.length; i++) {
      const testLine = currentLine + ' ' + words[i];
      if (ctx.measureText(testLine).width > maxLineWidth) {
        lines.push(currentLine);
        currentLine = words[i];
      } else {
        currentLine = testLine;
      }
    }
    lines.push(currentLine);

    const lineHeight = fontSize * 1.35;
    const blockHeight = lines.length * lineHeight;
    let startY = textY - (blockHeight / 2) + (lineHeight / 2);

    lines.forEach((line) => {
      const textMetrics = ctx.measureText(line);
      const textWidth = textMetrics.width;
      const padX = fontSize * 0.6;
      const padY = fontSize * 0.35;
      const bgX = (cw / 2) - (textWidth / 2) - padX;
      const bgY = startY - (lineHeight / 2) - padY;
      const bgW = textWidth + (padX * 2);
      const bgH = lineHeight + (padY * 2);

      if (hookStyle === 'yellow-black') {
        ctx.fillStyle = '#ffde00';
        ctx.beginPath();
        ctx.roundRect(bgX, bgY, bgW, bgH, 10);
        ctx.fill();
        ctx.lineWidth = Math.max(4, fontSize * 0.08);
        ctx.strokeStyle = '#000000';
        ctx.strokeText(line, cw / 2, startY);
        ctx.fillStyle = '#000000';
        ctx.fillText(line, cw / 2, startY);
      } else if (hookStyle === 'red-white') {
        ctx.fillStyle = '#dc2626';
        ctx.beginPath();
        ctx.roundRect(bgX, bgY, bgW, bgH, 8);
        ctx.fill();
        ctx.fillStyle = '#ffffff';
        ctx.fillText(line, cw / 2, startY);
      } else if (hookStyle === 'neon-cyan') {
        ctx.fillStyle = 'rgba(10, 12, 20, 0.85)';
        ctx.strokeStyle = '#06b6d4';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.roundRect(bgX, bgY, bgW, bgH, 8);
        ctx.fill();
        ctx.stroke();
        ctx.shadowColor = '#06b6d4';
        ctx.shadowBlur = 12;
        ctx.fillStyle = '#22d3ee';
        ctx.fillText(line, cw / 2, startY);
      } else {
        ctx.shadowColor = 'rgba(0, 0, 0, 0.8)';
        ctx.shadowBlur = 10;
        ctx.lineWidth = 6;
        ctx.strokeStyle = '#000000';
        ctx.strokeText(line, cw / 2, startY);
        ctx.fillStyle = '#ffffff';
        ctx.fillText(line, cw / 2, startY);
      }

      startY += lineHeight;
    });

    ctx.restore();
  }

  // ========================================================
  // Timeline Trimmer Interaction & Dragging
  // ========================================================
  function setupTimelineEvents() {
    if (!timelineTrack) return;

    startHandle.addEventListener('mousedown', (e) => startDrag(e, 'start'));
    startHandle.addEventListener('touchstart', (e) => startDrag(e, 'start'), { passive: false });

    endHandle.addEventListener('mousedown', (e) => startDrag(e, 'end'));
    endHandle.addEventListener('touchstart', (e) => startDrag(e, 'end'), { passive: false });

    timelineTrack.addEventListener('click', (e) => {
      if (isDragging) return;
      const rect = timelineTrack.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const pct = Math.max(0, Math.min(1, clickX / rect.width));
      const targetTime = pct * totalDuration;
      seekTo(targetTime);
    });

    window.addEventListener('mousemove', onDragMove);
    window.addEventListener('touchmove', onDragMove, { passive: false });
    window.addEventListener('mouseup', stopDrag);
    window.addEventListener('touchend', stopDrag);
  }

  function startDrag(e, handleType) {
    e.preventDefault();
    e.stopPropagation();
    isDragging = handleType;
  }

  function onDragMove(e) {
    if (!isDragging || totalDuration <= 0) return;
    e.preventDefault();

    const clientX = e.touches ? e.touches[0].clientX : e.clientX;
    const rect = timelineTrack.getBoundingClientRect();
    const pct = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const newTime = pct * totalDuration;

    const minClipDuration = 3;

    if (isDragging === 'start') {
      if (newTime < endTime - minClipDuration) {
        startTime = Math.max(0, newTime);
      } else {
        startTime = Math.max(0, endTime - minClipDuration);
      }
      seekTo(startTime);
    } else if (isDragging === 'end') {
      if (newTime > startTime + minClipDuration) {
        endTime = Math.min(totalDuration, newTime);
      } else {
        endTime = Math.min(totalDuration, startTime + minClipDuration);
      }
      seekTo(endTime);
    }

    updateTimelineUI();
    updateTimeDisplays(activeMode === 'youtube' && ytPlayer && ytPlayer.getCurrentTime ? ytPlayer.getCurrentTime() : videoEl.currentTime);
  }

  function stopDrag() {
    isDragging = null;
  }

  function updateTimelineUI() {
    if (!timelineTrack || totalDuration <= 0) return;

    const startPct = (startTime / totalDuration) * 100;
    const endPct = (endTime / totalDuration) * 100;

    startHandle.style.left = `${startPct}%`;
    endHandle.style.left = `${endPct}%`;

    segmentHighlight.style.left = `${startPct}%`;
    segmentHighlight.style.width = `${endPct - startPct}%`;

    startTimeLabel.textContent = formatTime(startTime);
    endTimeLabel.textContent = formatTime(endTime);
    selectedClipLength.textContent = `${Math.round(endTime - startTime)}s`;
  }

  function updatePlayheadPosition(currTime) {
    if (!timelinePlayhead || totalDuration <= 0) return;
    const t = currTime !== undefined ? currTime : (activeMode === 'youtube' && ytPlayer && ytPlayer.getCurrentTime ? ytPlayer.getCurrentTime() : videoEl.currentTime);
    const pct = (t / totalDuration) * 100;
    timelinePlayhead.style.left = `${pct}%`;
  }

  function updateTimeDisplays(currTime) {
    const t = currTime !== undefined ? currTime : (activeMode === 'youtube' && ytPlayer && ytPlayer.getCurrentTime ? ytPlayer.getCurrentTime() : videoEl.currentTime);
    if (currentTimeDisplay) {
      currentTimeDisplay.textContent = formatTime(t);
    }
    if (totalDurationDisplay) {
      totalDurationDisplay.textContent = formatTime(totalDuration);
    }
  }

  function seekTo(timeInSeconds) {
    const t = Math.max(0, Math.min(totalDuration, timeInSeconds));
    if (activeMode === 'youtube' && ytPlayer && typeof ytPlayer.seekTo === 'function') {
      ytPlayer.seekTo(t, true);
    } else if (videoEl) {
      videoEl.currentTime = t;
    }
  }

  function togglePlay() {
    if (activeMode === 'youtube' && ytPlayer && typeof ytPlayer.getPlayerState === 'function') {
      const state = ytPlayer.getPlayerState();
      if (state === 1) { // playing
        ytPlayer.pauseVideo();
      } else {
        const curr = ytPlayer.getCurrentTime();
        if (curr >= endTime) ytPlayer.seekTo(startTime, true);
        ytPlayer.playVideo();
      }
    } else if (videoEl) {
      if (videoEl.paused) {
        if (videoEl.currentTime >= endTime) videoEl.currentTime = startTime;
        videoEl.play();
      } else {
        videoEl.pause();
      }
    }
  }

  function replayClip() {
    seekTo(startTime);
    if (activeMode === 'youtube' && ytPlayer && ytPlayer.playVideo) {
      ytPlayer.playVideo();
    } else if (videoEl) {
      videoEl.play();
    }
  }

  function toggleMute() {
    if (activeMode === 'youtube' && ytPlayer && typeof ytPlayer.isMuted === 'function') {
      const muted = ytPlayer.isMuted();
      if (muted) ytPlayer.unMute();
      else ytPlayer.mute();
      const muteIcon = document.querySelector('#muteBtn i');
      if (muteIcon) muteIcon.className = muted ? 'fa-solid fa-volume-high' : 'fa-solid fa-volume-xmark';
    } else if (videoEl) {
      videoEl.muted = !videoEl.muted;
      const muteIcon = document.querySelector('#muteBtn i');
      if (muteIcon) muteIcon.className = videoEl.muted ? 'fa-solid fa-volume-xmark' : 'fa-solid fa-volume-high';
    }
  }

  function setClipSegment(startSec, endSec) {
    startTime = Math.max(0, startSec);
    endTime = Math.min(totalDuration || 3600, endSec);
    seekTo(startTime);
    updateTimelineUI();
    updateTimeDisplays(startTime);
  }

  function setClipDuration(seconds) {
    const dur = Math.min(Math.max(seconds, 5), totalDuration || 60);
    endTime = Math.min(totalDuration || 60, startTime + dur);
    if (endTime - startTime < dur) {
      startTime = Math.max(0, endTime - dur);
    }
    updateTimelineUI();
    updateTimeDisplays(startTime);
  }

  function setAspect(mode) {
    aspectMode = mode;
    const phone = document.getElementById('phoneFrame');
    if (phone) {
      phone.className = `smartphone-frame ratio-${mode.replace(':', '-')}`;
    }
    setCanvasResolution();
  }

  function setCropMode(mode) {
    cropMode = mode;
  }

  function setHookOverlay({ text, style, position, enabled }) {
    if (text !== undefined) hookText = text;
    if (style !== undefined) hookStyle = style;
    if (position !== undefined) hookPosition = position;
    if (enabled !== undefined) showHookOverlay = enabled;

    // Update live DOM banner for YouTube mode
    if (liveHookBanner) {
      liveHookBanner.textContent = hookText;
      liveHookBanner.className = `live-hook-overlay hook-style-${hookStyle} hook-pos-${hookPosition}`;
      liveHookBanner.style.display = showHookOverlay ? 'block' : 'none';
    }
  }

  // ========================================================
  // Client-Side Video Export & Rendering
  // ========================================================
  async function exportClip({ onProgress, onComplete, onError }) {
    if (activeMode === 'youtube') {
      // In YouTube embed mode, provide instant resolution or guide
      if (onError) {
        onError('To download this YouTube short video, please drop the downloaded video file into "Upload Local Video" tab or use direct MP4 download.');
      }
      return;
    }

    if (!videoEl || !canvas || totalDuration <= 0) {
      if (onError) onError('No video loaded to export.');
      return;
    }

    try {
      videoEl.pause();
      videoEl.currentTime = startTime;

      const fps = 30;
      const canvasStream = canvas.captureStream(fps);

      let audioTracks = [];
      try {
        if (typeof videoEl.captureStream === 'function') {
          audioTracks = videoEl.captureStream().getAudioTracks();
        } else if (typeof videoEl.mozCaptureStream === 'function') {
          audioTracks = videoEl.mozCaptureStream().getAudioTracks();
        }
      } catch (err) {
        console.warn('Audio capture stream fallback:', err);
      }

      const combinedTracks = [...canvasStream.getVideoTracks(), ...audioTracks];
      const combinedStream = new MediaStream(combinedTracks);

      let mimeType = 'video/webm;codecs=vp9';
      if (MediaRecorder.isTypeSupported('video/mp4;codecs=avc1')) {
        mimeType = 'video/mp4;codecs=avc1';
      } else if (MediaRecorder.isTypeSupported('video/webm;codecs=vp8,opus')) {
        mimeType = 'video/webm;codecs=vp8,opus';
      } else if (MediaRecorder.isTypeSupported('video/webm')) {
        mimeType = 'video/webm';
      }

      const recorder = new MediaRecorder(combinedStream, {
        mimeType: mimeType,
        videoBitsPerSecond: 6000000
      });

      const chunks = [];
      recorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) chunks.push(e.data);
      };

      const clipDuration = endTime - startTime;

      recorder.onstop = () => {
        const isMp4 = mimeType.includes('mp4');
        const blob = new Blob(chunks, { type: mimeType });
        const url = URL.createObjectURL(blob);
        const fileName = `ViralShort_${Math.round(clipDuration)}s_${Date.now()}.${isMp4 ? 'mp4' : 'webm'}`;

        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);

        videoEl.currentTime = startTime;
        if (onComplete) onComplete({ blob, url, fileName });
      };

      recorder.start(100);
      videoEl.play();

      const progressInterval = setInterval(() => {
        const currentProgress = videoEl.currentTime - startTime;
        const pct = Math.min(100, Math.round((currentProgress / clipDuration) * 100));
        if (onProgress) onProgress(pct);

        if (videoEl.currentTime >= endTime || videoEl.ended) {
          clearInterval(progressInterval);
          recorder.stop();
          videoEl.pause();
        }
      }, 100);

    } catch (exportErr) {
      console.error('Export failed:', exportErr);
      if (onError) onError(exportErr.message || 'Export failed.');
    }
  }

  function formatTime(sec) {
    const s = Math.floor(sec || 0);
    const mins = Math.floor(s / 60);
    const secs = s % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }

  return {
    init,
    loadSource,
    loadFile,
    loadYouTubeVideo,
    togglePlay,
    replayClip,
    toggleMute,
    seekTo,
    setClipSegment,
    setClipDuration,
    setAspect,
    setCropMode,
    setHookOverlay,
    exportClip,
    getStartTime: () => startTime,
    getEndTime: () => endTime,
    getTotalDuration: () => totalDuration,
    getActiveMode: () => activeMode
  };
})();
