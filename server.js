/**
 * Local Development Server (Zero-dependency Node.js)
 * Run: node server.js
 * Open: http://localhost:3000
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const https = require('https');

const PORT = 3000;
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.mp4': 'video/mp4',
  '.webm': 'video/webm'
};

const server = http.createServer(async (req, res) => {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  const parsedUrl = new URL(req.url, `http://localhost:${PORT}`);
  const pathname = parsedUrl.pathname;

  // Mock / Dev API routes when testing locally without PHP
  if (pathname === '/api/info.php') {
    const ytUrl = parsedUrl.searchParams.get('url') || '';
    const videoIdMatch = ytUrl.match(/(?:shorts\/|v=|\/v\/|youtu\.be\/|\/embed\/)([a-zA-Z0-9_-]{11})/i);
    const videoId = videoIdMatch ? videoIdMatch[1] : null;

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      url: ytUrl,
      video_id: videoId,
      title: videoId ? `YouTube Video [${videoId}]` : 'Sample Video',
      author: 'Creator Studio',
      thumbnail: videoId ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : '',
      duration: 180,
      source: 'local-node-dev'
    }));
    return;
  }

  if (pathname === '/api/suggest.php') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      let data = {};
      try { data = JSON.parse(body); } catch(e) {}

      const title = data.title || 'Creator Video';
      const duration = parseInt(data.duration, 10) || 180;
      const targetDuration = parseInt(data.target_duration, 10) || 30;

      const s1End = Math.min(targetDuration, duration);
      const s2Start = Math.min(Math.round(duration * 0.35), duration - targetDuration);
      const s2End = Math.min(s2Start + targetDuration, duration);
      const s3Start = Math.min(Math.round(duration * 0.70), duration - targetDuration);
      const s3End = Math.min(s3Start + targetDuration, duration);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        status: 'success',
        source: 'local-dev-analyzer',
        data: {
          premise: `Pacing-optimized viral segments extracted for "${title}"`,
          clips: [
            {
              id: 1,
              title: '💥 Explosive Opening Hook',
              start_seconds: 0,
              end_seconds: s1End,
              duration: s1End,
              virality_score: 97,
              hook_reason: 'Scroll-stopping opening frame with immediate visual mystery.',
              caption_overlay: 'YOU WON\'T BELIEVE THIS... 🤯',
              hashtags: ['#Shorts', '#Viral', '#Trending']
            },
            {
              id: 2,
              title: '🔥 The Golden Secret Revealed',
              start_seconds: Math.max(0, s2Start),
              end_seconds: s2End,
              duration: Math.round(s2End - s2Start),
              virality_score: 93,
              hook_reason: 'High-density moment where the main curiosity loop gets resolved.',
              caption_overlay: 'THE SECRET NOBODY SHARES ⚡',
              hashtags: ['#MustWatch', '#LifeHacks', '#FYP']
            },
            {
              id: 3,
              title: '🎯 The Final Payoff & Twist',
              start_seconds: Math.max(0, s3Start),
              end_seconds: s3End,
              duration: Math.round(s3End - s3Start),
              virality_score: 90,
              hook_reason: 'Strong emotional punchline that incentivizes shares and saves.',
              caption_overlay: 'SAVE THIS BEFORE IT\'S GONE! 📌',
              hashtags: ['#ShortsFeed', '#ViralVideo', '#Tips']
            }
          ]
        }
      }));
    });
    return;
  }

  if (pathname === '/api/auth.php') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      let data = {};
      try { data = JSON.parse(body); } catch(e) {}
      const action = parsedUrl.searchParams.get('action') || data.action || 'login';
      const email = data.email || 'creator@viralclip.com';
      const name = data.name || email.split('@')[0];

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        status: 'success',
        user: { id: 1, name: name, email: email },
        token: 'dev-token-xyz'
      }));
    });
    return;
  }

  // Static File Serving
  let filePath = path.join(__dirname, pathname === '/' ? 'index.html' : pathname);
  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      if (err.code === 'ENOENT') {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
      } else {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end(`Server Error: ${err.code}`);
      }
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

server.listen(PORT, () => {
  console.log(`=======================================================`);
  console.log(`🚀 ViralClip Studio Local Dev Server running!`);
  console.log(`👉 Open: http://localhost:${PORT}`);
  console.log(`=======================================================`);
});
