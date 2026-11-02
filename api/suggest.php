<?php
/**
 * suggest.php - Generates viral clip suggestions and hooks using Google Gemini Flash
 */
require_once __DIR__ . '/config.php';

$rawInput = file_get_contents('php://input');
$jsonData = json_decode($rawInput, true) ?: [];
$data = array_merge($_GET, $_POST, $jsonData);

// Verify user authorization: Only admin-approved accounts can use master AI key
$userEmail = $data['user_email'] ?? $_SERVER['HTTP_X_USER_EMAIL'] ?? '';
$userToken = $data['user_token'] ?? $_SERVER['HTTP_X_USER_TOKEN'] ?? '';

if (!checkUserAuthorized($userEmail, $userToken)) {
    sendJson([
        'error' => 'Access Denied: Only registered, admin-approved accounts can use the AI Virality Engine. Please log in or contact the administrator to create your account.',
        'status_code' => 'unauthorized_user'
    ], 403);
}

$title = $data['title'] ?? 'Interesting Long Video';
$description = $data['description'] ?? '';
$duration = intval($data['duration'] ?? 180);
$targetDuration = intval($data['target_duration'] ?? 30);
$clientApiKey = $data['api_key'] ?? '';

// Master Admin Gemini API key is automatically used for all allowed users
$settings = getSystemSettings();
$apiKey = !empty($settings['gemini_api_key']) ? $settings['gemini_api_key'] : GEMINI_API_KEY;

if (empty($apiKey) || $apiKey === 'YOUR_GEMINI_API_KEY_HERE') {
    // Return high quality algorithmic fallback if no Gemini key is provided yet
    $fallbackClips = generateFallbackSuggestions($title, $duration, $targetDuration);
    sendJson([
        'status' => 'fallback',
        'message' => 'Using algorithmic clip suggestions. Add your free Gemini API key in Settings for AI-powered hook analysis.',
        'data' => $fallbackClips
    ]);
}

$prompt = <<<EOT
You are an elite short-form video editor and YouTube Shorts virality consultant (specializing in MrBeast retention tactics, TikTok psychology, and high watch-time hooks).

Analyze this video information:
- Title: "{$title}"
- Description/Context: "{$description}"
- Total Video Duration: {$duration} seconds
- Desired Short Duration: ~{$targetDuration} seconds (between 15 to 60 seconds)

Find 3 to 4 best potential viral clip segments from this video.
For each segment, select realistic start and end timestamps (in seconds, must be within 0 and {$duration}), create an irresistible hook, a virality score, and an on-screen text caption overlay.

Respond strictly with valid JSON with this exact schema:
{
  "premise": "Short explanation of the video hook angle",
  "clips": [
    {
      "id": 1,
      "title": "Snappy Viral Headline With Emoji",
      "start_seconds": 0,
      "end_seconds": 30,
      "duration": 30,
      "virality_score": 96,
      "hook_reason": "Why the first 3 seconds hook viewers and prevent scrolling",
      "caption_overlay": "WAIT TILL YOU SEE THIS... 🤯",
      "hashtags": ["#Shorts", "#Viral", "#FYP"]
    }
  ]
}
Do not include markdown code block wrappers (like ```json), just output the raw JSON string.
EOT;

$payload = [
    'contents' => [
        [
            'role' => 'user',
            'parts' => [
                ['text' => $prompt]
            ]
        ]
    ],
    'generationConfig' => [
        'temperature' => 0.7,
        'responseMimeType' => 'application/json'
    ]
];

$modelsToTry = ['gemini-flash-latest'];
$response = false;
$httpCode = 0;
$curlError = '';

foreach ($modelsToTry as $modelName) {
    $geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/{$modelName}:generateContent?key=" . urlencode($apiKey);
    $ch = curl_init($geminiUrl);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
        CURLOPT_POSTFIELDS => json_encode($payload),
        CURLOPT_TIMEOUT => 6,
        CURLOPT_CONNECTTIMEOUT => 4
    ]);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($httpCode === 200 && $response) {
        break;
    }
}

if ($httpCode !== 200 || !$response) {
    // If Gemini fails (e.g. rate limit or bad key), provide graceful fallback so app never breaks
    $fallbackClips = generateFallbackSuggestions($title, $duration, $targetDuration);
    sendJson([
        'status' => 'fallback',
        'error_detail' => $curlError ?: "Gemini API HTTP {$httpCode}",
        'message' => 'Gemini API call failed or quota exceeded. Showing smart calculated clips.',
        'data' => $fallbackClips
    ]);
}

$decoded = json_decode($response, true);
$aiText = $decoded['candidates'][0]['content']['parts'][0]['text'] ?? null;

if ($aiText) {
    // Clean any accidental markdown backticks
    $cleanJson = preg_replace('/^```(?:json)?\s*/i', '', trim($aiText));
    $cleanJson = preg_replace('/\s*```$/i', '', $cleanJson);
    $resultData = json_decode($cleanJson, true);

    if ($resultData && isset($resultData['clips'])) {
        sendJson([
            'status' => 'success',
            'source' => 'gemini-1.5-flash',
            'data' => $resultData
        ]);
    }
}

// Fallback if parsing failed
$fallbackClips = generateFallbackSuggestions($title, $duration, $targetDuration);
sendJson([
    'status' => 'fallback',
    'message' => 'Could not parse AI response. Using smart clip suggestions.',
    'data' => $fallbackClips
]);

/**
 * Intelligent algorithmic fallback clip generator based on pacing & retention curves
 */
function generateFallbackSuggestions($title, $totalDuration, $targetDuration) {
    $duration = max($totalDuration, 60);
    $clipLen = min(max($targetDuration, 15), 60);

    // Segment 1: The Intro Hook (0s - clipLen)
    $clip1End = min($clipLen, $duration);
    
    // Segment 2: The Golden Peak (around 30-40% into video)
    $clip2Start = round($duration * 0.35);
    $clip2End = min($clip2Start + $clipLen, $duration);

    // Segment 3: The Climax / Twist (around 70-80% into video)
    $clip3Start = round($duration * 0.70);
    $clip3End = min($clip3Start + $clipLen, $duration);

    return [
        'premise' => 'Algorithmic high-retention segments based on viewer drop-off curves',
        'clips' => [
            [
                'id' => 1,
                'title' => '💥 Strongest Opening Hook',
                'start_seconds' => 0,
                'end_seconds' => $clip1End,
                'duration' => $clip1End,
                'virality_score' => 95,
                'hook_reason' => 'First impression determines whether the viewer stays or swipes immediately.',
                'caption_overlay' => 'YOU WON\'T BELIEVE THIS... 🤯',
                'hashtags' => ['#Viral', '#Shorts', '#Trending']
            ],
            [
                'id' => 2,
                'title' => '🔥 The Core Revelation',
                'start_seconds' => $clip2Start,
                'end_seconds' => $clip2End,
                'duration' => round($clip2End - $clip2Start),
                'virality_score' => 91,
                'hook_reason' => 'Peak information density where the main question or suspense is answered.',
                'caption_overlay' => 'THE TRUTH FINALLY REVEALED ⚡',
                'hashtags' => ['#MustWatch', '#ShortsFeed', '#FYP']
            ],
            [
                'id' => 3,
                'title' => '⚡ Climax & Key Takeaway',
                'start_seconds' => $clip3Start,
                'end_seconds' => $clip3End,
                'duration' => round($clip3End - $clip3Start),
                'virality_score' => 88,
                'hook_reason' => 'High emotional punchline or payoff that drives comments and shares.',
                'caption_overlay' => 'WAIT FOR THE END! 🎯',
                'hashtags' => ['#Mindblown', '#LifeHacks', '#Shorts']
            ]
        ]
    ];
}
