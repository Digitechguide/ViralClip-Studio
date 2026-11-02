<?php
require_once __DIR__ . '/config.php';

$url = isset($_GET['url']) ? trim($_GET['url']) : '';

if (empty($url)) {
    sendJson(['error' => 'Missing "url" query parameter'], 400);
}

// Validate basic YouTube URL structure
if (!preg_match('/^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be)\/.+$/i', $url)) {
    sendJson(['error' => 'Please provide a valid YouTube or YouTube Shorts URL'], 400);
}

// Extract Video ID
function extractYouTubeId($url) {
    if (preg_match('/(?:shorts\/|v=|\/v\/|youtu\.be\/|\/embed\/)([a-zA-Z0-9_-]{11})/i', $url, $matches)) {
        return $matches[1];
    }
    return null;
}

$videoId = extractYouTubeId($url);

$response = [
    'url' => $url,
    'video_id' => $videoId,
    'title' => 'YouTube Video',
    'author' => 'Creator',
    'thumbnail' => $videoId ? "https://img.youtube.com/vi/{$videoId}/maxresdefault.jpg" : '',
    'duration' => null,
    'source' => 'oembed'
];

// 1. First attempt: YouTube oEmbed (standard, light, works 100% on any shared hosting with no binaries needed)
$cleanWatchUrl = $videoId ? "https://www.youtube.com/watch?v={$videoId}" : $url;
$oembedUrl = "https://www.youtube.com/oembed?url=" . urlencode($cleanWatchUrl) . "&format=json";
$context = stream_context_create([
    'http' => [
        'timeout' => 5,
        'user_agent' => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
    ]
]);

$rawOembed = @file_get_contents($oembedUrl, false, $context);
if ($rawOembed) {
    $oembedData = json_decode($rawOembed, true);
    if ($oembedData) {
        $response['title'] = $oembedData['title'] ?? $response['title'];
        $response['author'] = $oembedData['author_name'] ?? $response['author'];
        $response['thumbnail'] = $oembedData['thumbnail_url'] ?? $response['thumbnail'];
    }
}

// 2. Second attempt: Check if yt-dlp is available for full metadata and exact duration
if (file_exists(YT_DLP_PATH) && is_executable(YT_DLP_PATH)) {
    $cmd = escapeshellcmd(YT_DLP_PATH) . ' --dump-json --skip-download --no-warnings ' . escapeshellarg($url);
    $output = @shell_exec($cmd);
    if ($output) {
        $details = json_decode($output, true);
        if ($details) {
            $response['title'] = $details['title'] ?? $response['title'];
            $response['author'] = $details['uploader'] ?? $response['author'];
            $response['duration'] = $details['duration'] ?? null;
            $response['description'] = substr($details['description'] ?? '', 0, 1000);
            $response['thumbnail'] = $details['thumbnail'] ?? $response['thumbnail'];
            $response['source'] = 'yt-dlp';
        }
    }
}

sendJson($response);
