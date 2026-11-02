/**
 * YouTube Helper Module
 * Handles URL extraction, metadata queries, and proxy streaming
 */
const YouTubeHelper = (function() {
  'use strict';

  function extractVideoId(url) {
    if (!url) return null;
    const cleanUrl = url.trim();

    // Match Shorts: youtube.com/shorts/{id}
    const shortsMatch = cleanUrl.match(/\/shorts\/([a-zA-Z0-9_-]{11})/i);
    if (shortsMatch) return shortsMatch[1];

    // Match standard watch URL: youtube.com/watch?v={id}
    const watchMatch = cleanUrl.match(/[?&]v=([a-zA-Z0-9_-]{11})/i);
    if (watchMatch) return watchMatch[1];

    // Match short url: youtu.be/{id}
    const youtuBeMatch = cleanUrl.match(/youtu\.be\/([a-zA-Z0-9_-]{11})/i);
    if (youtuBeMatch) return youtuBeMatch[1];

    // Match embed: youtube.com/embed/{id}
    const embedMatch = cleanUrl.match(/\/embed\/([a-zA-Z0-9_-]{11})/i);
    if (embedMatch) return embedMatch[1];

    return null;
  }

  function isValidYouTubeUrl(url) {
    return extractVideoId(url) !== null;
  }

  async function fetchMetadata(url, apiBase = 'api') {
    const videoId = extractVideoId(url);
    if (!videoId) {
      throw new Error('Please enter a valid YouTube or YouTube Shorts URL.');
    }

    const standardUrl = `https://www.youtube.com/watch?v=${videoId}`;

    // Try backend api/info.php first if available
    try {
      const response = await fetch(`${apiBase}/info.php?url=${encodeURIComponent(standardUrl)}`);
      if (response.ok) {
        const data = await response.json();
        if (data && !data.error) {
          return {
            id: videoId,
            title: data.title || 'YouTube Video',
            author: data.author || 'Creator',
            thumbnail: data.thumbnail || `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`,
            duration: data.duration || 180,
            description: data.description || '',
            url: url
          };
        }
      }
    } catch (e) {
      console.warn('Backend api/info.php unavailable or offline, attempting client-side oEmbed...', e);
    }

    // Fallback: Direct YouTube oEmbed API (100% client-side, zero backend needed)
    try {
      const oembedRes = await fetch(`https://www.youtube.com/oembed?url=${encodeURIComponent(standardUrl)}&format=json`);
      if (oembedRes.ok) {
        const oembedData = await oembedRes.json();
        return {
          id: videoId,
          title: oembedData.title || 'YouTube Video',
          author: oembedData.author_name || 'Creator',
          thumbnail: `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`,
          duration: 180,
          description: `Video by ${oembedData.author_name}`,
          url: url
        };
      }
    } catch (err) {
      console.warn('Direct oEmbed failed, falling back to minimal defaults', err);
    }

    // Ultimate fallback based on ID
    return {
      id: videoId,
      title: `YouTube Video (${videoId})`,
      author: 'YouTube Creator',
      thumbnail: `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`,
      duration: 180,
      description: '',
      url: url
    };
  }

  function getStreamUrl(url, apiBase = 'api') {
    return `${apiBase}/download.php?url=${encodeURIComponent(url)}`;
  }

  return {
    extractVideoId,
    isValidYouTubeUrl,
    fetchMetadata,
    getStreamUrl
  };
})();
