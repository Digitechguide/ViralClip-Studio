<?php
// Secure endpoint to synchronize latest GitHub Release APK directly to server storage
// Protected by admin session or secret key

header('Content-Type: application/json');

$key = $_GET['key'] ?? '';
if ($key !== 'sync_salicylates_2026') {
    http_response_code(403);
    echo json_encode(['error' => 'Unauthorized']);
    exit;
}

$tag = $_GET['tag'] ?? 'v1.0.21';
$remoteUrl = "https://github.com/Digitechguide/ViralClip-Studio/releases/download/{$tag}/app-release.apk";
$destPath = dirname(__DIR__) . "/ViralClip-Studio.apk";
$tempPath = dirname(__DIR__) . "/ViralClip-Studio.apk.tmp";

// Use streaming cURL with redirect follow to download directly from GitHub CDN
$fp = fopen($tempPath, 'w+');
if (!$fp) {
    echo json_encode(['error' => 'Cannot create temp file']);
    exit;
}

$ch = curl_init($remoteUrl);
curl_setopt($ch, CURLOPT_FILE, $fp);
curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 300);
curl_setopt($ch, CURLOPT_USERAGENT, 'ViralClip-Server/1.0');
$success = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$error = curl_error($ch);
curl_close($ch);
fclose($fp);

if ($success && $httpCode >= 200 && $httpCode < 300 && filesize($tempPath) > 1000000) {
    if (file_exists($destPath)) {
        unlink($destPath);
    }
    rename($tempPath, $destPath);
    echo json_encode([
        'status' => 'success',
        'tag' => $tag,
        'bytes' => filesize($destPath),
        'md5' => md5_file($destPath)
    ]);
} else {
    if (file_exists($tempPath)) {
        unlink($tempPath);
    }
    echo json_encode([
        'status' => 'error',
        'http_code' => $httpCode,
        'curl_error' => $error,
        'url' => $remoteUrl
    ]);
}
