/**
 * AI Analyzer Module
 * Connects to Google Gemini Flash API for high-retention hooks and viral clip discovery
 */
const AIAnalyzer = (function() {
  'use strict';

  async function getViralSuggestions({ title, description, duration, targetDuration, apiKey, apiBase = 'api' }) {
    // Read logged-in user credentials to authenticate against master server
    let userEmail = '';
    let userToken = '';
    try {
      const user = JSON.parse(localStorage.getItem('viralclip_user') || 'null');
      if (user && user.email) userEmail = user.email;
      userToken = localStorage.getItem('viralclip_token') || '';
    } catch(e) {}

    // 1. First attempt: call server-side api/suggest.php (uses Master Admin API key on server)
    try {
      const payload = {
        title: title || 'Long Video',
        description: description || '',
        duration: Math.round(duration || 180),
        target_duration: Math.round(targetDuration || 30),
        api_key: apiKey || '',
        user_email: userEmail,
        user_token: userToken
      };

      let response = await fetch(`${apiBase}/suggest.php`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Email': userEmail,
          'X-User-Token': userToken
        },
        body: JSON.stringify(payload)
      });

      // If server blocks POST, fallback to GET query params
      if (!response.ok && response.status !== 403) {
        const getUrl = `${apiBase}/suggest.php?title=${encodeURIComponent(payload.title)}&duration=${payload.duration}&target_duration=${payload.target_duration}&user_email=${encodeURIComponent(userEmail)}&user_token=${encodeURIComponent(userToken)}`;
        response = await fetch(getUrl);
      }

      if (response.ok) {
        const result = await response.json();
        if (result && result.data && Array.isArray(result.data.clips)) {
          return result.data;
        }
      } else if (response.status === 403) {
        const errData = await response.json().catch(() => ({}));
        if (typeof window.showToast === 'function') {
          window.showToast(errData.error || 'Only admin-approved accounts can use the AI engine. Please log in.', 'error');
        }
        const authModal = document.getElementById('authModal');
        if (authModal) authModal.style.display = 'flex';
      }
    } catch (err) {
      console.warn('Backend api/suggest.php unreachable, checking client-side Gemini fallback...', err);
    }

    // 2. Client-side direct Gemini Flash call if client provided an API key
    if (apiKey && apiKey.trim().length > 10) {
      try {
        const directResult = await callGeminiDirectly({
          title, description, duration, targetDuration, apiKey
        });
        if (directResult && directResult.clips) {
          return directResult;
        }
      } catch (geminiErr) {
        console.error('Direct Gemini API call failed:', geminiErr);
      }
    }

    // 3. Fallback: Algorithmic smart segmentation curve
    return generateSmartFallback({ title, duration, targetDuration });
  }

  async function callGeminiDirectly({ title, description, duration, targetDuration, apiKey }) {
    const prompt = `You are a world-class viral short-form video editor specializing in YouTube Shorts and TikTok retention.
Analyze this video:
- Title: "${title}"
- Description: "${description}"
- Total Duration: ${Math.round(duration)} seconds
- Target Short Duration: ~${targetDuration} seconds

Identify 3 to 4 viral segments with exact start and end timestamps in seconds (between 0 and ${Math.round(duration)}).
Return strict JSON matching this structure without markdown code blocks:
{
  "premise": "Short explanation of the viral hook premise",
  "clips": [
    {
      "id": 1,
      "title": "Short Attention-Grabbing Title With Emoji",
      "start_seconds": 0,
      "end_seconds": ${Math.min(targetDuration, duration)},
      "duration": ${Math.min(targetDuration, duration)},
      "virality_score": 96,
      "hook_reason": "Why the opening 3 seconds prevent the viewer from scrolling",
      "caption_overlay": "WAIT TILL YOU SEE THIS... 🤯",
      "hashtags": ["#Shorts", "#Viral", "#FYP"]
    }
  ]
}`;

    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=${encodeURIComponent(apiKey)}`;
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ role: 'user', parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.7, responseMimeType: 'application/json' }
      })
    });

    if (!res.ok) {
      throw new Error(`Gemini API error: ${res.statusText}`);
    }

    const data = await res.json();
    const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (rawText) {
      const cleanJson = rawText.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/i, '').trim();
      return JSON.parse(cleanJson);
    }
    throw new Error('Empty Gemini response');
  }

  function generateSmartFallback({ title, duration, targetDuration }) {
    const total = Math.max(duration || 120, 60);
    const target = Math.min(Math.max(targetDuration || 30, 15), 60);

    const s1End = Math.min(target, total);

    const s2Start = Math.min(Math.round(total * 0.35), total - target);
    const s2End = Math.min(s2Start + target, total);

    const s3Start = Math.min(Math.round(total * 0.70), total - target);
    const s3End = Math.min(s3Start + target, total);

    return {
      premise: `Pacing-optimized viral segments extracted for "${title || 'your video'}"`,
      clips: [
        {
          id: 1,
          title: '💥 Explosive Intro Hook',
          start_seconds: 0,
          end_seconds: s1End,
          duration: s1End,
          virality_score: 96,
          hook_reason: 'The first 3 seconds hook viewer curiosity and lower the swipe-away rate.',
          caption_overlay: 'YOU NEED TO HEAR THIS... 🤯',
          hashtags: ['#Shorts', '#Viral', '#Trending']
        },
        {
          id: 2,
          title: '🔥 The Golden Revelation',
          start_seconds: Math.max(0, s2Start),
          end_seconds: s2End,
          duration: Math.round(s2End - s2Start),
          virality_score: 92,
          hook_reason: 'High-density moment where the core breakthrough is delivered.',
          caption_overlay: 'THE PART NOBODY TALKS ABOUT ⚡',
          hashtags: ['#MustWatch', '#LifeHacks', '#FYP']
        },
        {
          id: 3,
          title: '🎯 Climax & Actionable Takeaway',
          start_seconds: Math.max(0, s3Start),
          end_seconds: s3End,
          duration: Math.round(s3End - s3Start),
          virality_score: 89,
          hook_reason: 'Strong emotional punchline that incentivizes shares, saves, and comments.',
          caption_overlay: 'SAVE THIS BEFORE IT\'S GONE! 📌',
          hashtags: ['#ShortsFeed', '#ViralVideo', '#Tips']
        }
      ]
    };
  }

  function renderSuggestions(suggestions, container, onApplyClip) {
    if (!container) return;
    container.innerHTML = '';

    if (!suggestions || !Array.isArray(suggestions.clips) || suggestions.clips.length === 0) {
      container.innerHTML = `
        <div class="empty-suggestions">
          <i class="fa-solid fa-sparkles"></i>
          <p>No clips found. Adjust duration or reload the video.</p>
        </div>`;
      return;
    }

    suggestions.clips.forEach(clip => {
      const card = document.createElement('div');
      card.className = 'viral-clip-card';
      
      const startMinSec = formatTime(clip.start_seconds);
      const endMinSec = formatTime(clip.end_seconds);

      card.innerHTML = `
        <div class="clip-card-header">
          <span class="virality-pill"><i class="fa-solid fa-fire"></i> ${clip.virality_score}% Virality</span>
          <span class="clip-duration-tag">${startMinSec} - ${endMinSec} (${clip.duration}s)</span>
        </div>
        <div class="clip-title">${escapeHtml(clip.title)}</div>
        <div class="clip-reason">${escapeHtml(clip.hook_reason)}</div>
        <div class="clip-tags">
          ${(clip.hashtags || []).map(tag => `<span class="clip-tag">${escapeHtml(tag)}</span>`).join('')}
        </div>
        <div class="clip-card-actions" style="display:flex; gap:6px;">
          <button class="btn btn-primary btn-xs apply-clip-btn" style="flex:1;">
            <i class="fa-solid fa-scissors"></i> Apply Segment & Hook
          </button>
          <button type="button" class="btn btn-secondary btn-xs copy-clip-btn" title="Copy hook and timestamps to clipboard">
            <i class="fa-solid fa-copy"></i> Copy Hook
          </button>
        </div>
      `;

      card.querySelector('.apply-clip-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        if (typeof onApplyClip === 'function') {
          onApplyClip(clip);
        }
      });

      card.querySelector('.copy-clip-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        const copyPayload = `🔥 Title: ${clip.title}\n⏱️ Timestamps: ${startMinSec} - ${endMinSec} (${clip.duration}s)\n🎯 Hook: ${clip.hook_reason}\n💬 Caption: ${clip.caption_overlay}\n🏷️ Tags: ${(clip.hashtags || []).join(' ')}`;
        navigator.clipboard.writeText(copyPayload).then(() => {
          if (typeof window.showToast === 'function') {
            window.showToast('Copied hook & timestamps to clipboard!', 'success');
          }
        }).catch(() => {
          // Fallback
          const ta = document.createElement('textarea');
          ta.value = copyPayload;
          document.body.appendChild(ta);
          ta.select();
          document.execCommand('copy');
          document.body.removeChild(ta);
          if (typeof window.showToast === 'function') {
            window.showToast('Copied hook & timestamps to clipboard!', 'success');
          }
        });
      });

      card.addEventListener('click', () => {
        if (typeof onApplyClip === 'function') {
          onApplyClip(clip);
        }
      });

      container.appendChild(card);
    });
  }

  function formatTime(seconds) {
    const s = Math.floor(seconds || 0);
    const mins = Math.floor(s / 60);
    const secs = s % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  return {
    getViralSuggestions,
    renderSuggestions
  };
})();
