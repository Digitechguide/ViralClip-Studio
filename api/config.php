<?php
/**
 * Configuration file for Viral Video Studio
 * Serverbyt.in Shared Hosting Configuration
 */

// Safe dynamic system settings helper
function getSystemSettings() {
    $localFile = __DIR__ . '/data/settings.json';
    $tempFile = sys_get_temp_dir() . '/viral_settings.json';
    
    $data = null;
    if (file_exists($localFile)) {
        $data = json_decode(@file_get_contents($localFile), true);
    } elseif (file_exists($tempFile)) {
        $data = json_decode(@file_get_contents($tempFile), true);
    }

    if (is_array($data) && !empty($data['gemini_api_key'])) {
        return $data;
    }

    // Default Master Admin API Key (Loaded from environment or settings.json)
    return [
        'gemini_api_key' => getenv('GEMINI_API_KEY') ?: 'YOUR_GEMINI_API_KEY_HERE'
    ];
}

function saveSystemSettings($settings) {
    $localDir = __DIR__ . '/data';
    if (!file_exists($localDir)) {
        @mkdir($localDir, 0777, true);
    }
    $localFile = $localDir . '/settings.json';
    $tempFile = sys_get_temp_dir() . '/viral_settings.json';
    $json = json_encode($settings, JSON_PRETTY_PRINT);

    if (!@file_put_contents($localFile, $json)) {
        @file_put_contents($tempFile, $json);
    }
}

// Master Admin Gemini API Key
$currentSettings = getSystemSettings();
define('GEMINI_API_KEY', $currentSettings['gemini_api_key']);

// Path to yt-dlp binary on the server
define('YT_DLP_PATH', __DIR__ . '/yt-dlp');

// Temporary directory for processing video clips
define('TEMP_DIR', sys_get_temp_dir() . '/viral_clips');

// Maximum allowed duration for downloaded long video (default 1 hour)
define('MAX_VIDEO_DURATION_SEC', 3600);

// Allow Cross-Origin Resource Sharing (CORS) for front-end access
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-User-Email, X-User-Token');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// User Authorization Helper: Only admin-approved accounts with valid token signatures can access backend resources
function checkUserAuthorized($email, $token = '') {
    $cleanEmail = strtolower(trim($email ?? ''));
    $cleanToken = trim($token ?? '');
    
    // Both email and signed token are strictly mandatory
    if (empty($cleanEmail) || empty($cleanToken)) {
        return false;
    }

    // Master admin requires valid admin token signature
    if ($cleanEmail === 'pallanti2020@gmail.com') {
        $adminPass = defined('ADMIN_PASS') ? ADMIN_PASS : 'Abhi@9246830084';
        $expectedAdminToken = base64_encode('1:' . hash('sha256', 'pallanti2020@gmail.com' . $adminPass));
        return hash_equals($expectedAdminToken, $cleanToken);
    }

    $localUsersFile = __DIR__ . '/data/users.json';
    $tempUsersFile = sys_get_temp_dir() . '/viral_users.json';
    $content = false;
    if (file_exists($localUsersFile)) {
        $content = @file_get_contents($localUsersFile);
    } elseif (file_exists($tempUsersFile)) {
        $content = @file_get_contents($tempUsersFile);
    }
    $users = json_decode($content, true);
    if (!is_array($users) || !isset($users[$cleanEmail])) {
        return false;
    }

    $user = $users[$cleanEmail];
    // Must be approved by administrator
    if (($user['status'] ?? '') !== 'approved') {
        return false;
    }

    // Verify cryptographic token signature against stored password hash
    $passSecret = $user['password_hash'] ?? ($user['password'] ?? '');
    $expectedToken = base64_encode($user['id'] . ':' . hash('sha256', $user['email'] . $passSecret));
    return hash_equals($expectedToken, $cleanToken);
}

// Helper to return JSON
function sendJson($data, $statusCode = 200) {
    http_response_code($statusCode);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data);
    exit;
}
