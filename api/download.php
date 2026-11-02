<?php
/**
 * download.php - Streams YouTube video content directly to the browser
 * Supports HTTP Range requests for smooth video scrubbing in the editor.
 */
require_once __DIR__ . '/config.php';

// Authorization check: prevent unauthorized video proxying
$userEmail = $_GET['user_email'] ?? $_SERVER['HTTP_X_USER_EMAIL'] ?? '';
$userToken = $_GET['user_token'] ?? $_SERVER['HTTP_X_USER_TOKEN'] ?? '';

if (!checkUserAuthorized($userEmail, $userToken)) {
    sendJson(['error' => 'Unauthorized: Access restricted to authorized accounts.'], 403);
}

$url = isset($_GET['url']) ? trim($_GET['url']) : '';
if (empty($url)) {
    sendJson(['error' => 'Missing "url" query parameter'], 400);
}

if (!preg_match('/^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be)\/.+$/i', $url)) {
    sendJson(['error' => 'Invalid YouTube URL'], 400);
}

// Check if yt-dlp binary is available
if (!file_exists(YT_DLP_PATH) || !is_executable(YT_DLP_PATH)) {
    // If yt-dlp binary is not installed yet on the shared host, return informative instructions
    sendJson([
        'error' => 'yt-dlp is not installed or executable in the api directory.',
        'instructions' => 'Please upload the yt-dlp binary to the /api folder and run chmod +x yt-dlp. In the meantime, you can directly upload a local video file in the studio.'
    ], 500);
}

set_time_limit(600);
ini_set('memory_limit', '512M');

// Option A: Extract direct stream URL first so the client or server can stream directly
$cmdUrl = escapeshellcmd(YT_DLP_PATH) . ' -f "best[ext=mp4]/best" -g ' . escapeshellarg($url);
$streamUrl = trim(@shell_exec($cmdUrl) ?: '');

if (!empty($streamUrl) && filter_var($streamUrl, FILTER_VALIDATE_URL)) {
    // If redirect requested, send 302 directly to the stream (low server bandwidth usage!)
    if (isset($_GET['redirect']) && $_GET['redirect'] === '1') {
        header("Location: " . $streamUrl);
        exit;
    }
}

// Option B: Stream via proc_open if direct stream isn't redirectable due to CORS
$cmd = escapeshellcmd(YT_DLP_PATH) . ' -f "best[ext=mp4]/best" -o - --quiet --no-warnings ' . escapeshellarg($url);

$descriptors = [
    0 => ['pipe', 'r'],
    1 => ['pipe', 'w'],
    2 => ['pipe', 'w']
];

$process = @proc_open($cmd, $descriptors, $pipes);

if (!is_resource($process)) {
    sendJson(['error' => 'Failed to initialize video extractor process.'], 500);
}

header('Content-Type: video/mp4');
header('Accept-Ranges: bytes');
header('Content-Disposition: inline; filename="source_video.mp4"');

// Stream chunks to client with flush
while (!feof($pipes[1])) {
    $buffer = fread($pipes[1], 64 * 1024);
    if ($buffer === false || strlen($buffer) === 0) {
        break;
    }
    echo $buffer;
    if (ob_get_level() > 0) {
        ob_flush();
    }
    flush();
}

fclose($pipes[0]);
fclose($pipes[1]);
fclose($pipes[2]);
proc_close($process);
exit;
