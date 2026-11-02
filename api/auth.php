<?php
/**
 * Authentication & Admin API for ViralClip Studio
 * Manages user authentication, admin authorization, and user approvals.
 */
require_once __DIR__ . '/config.php';

// Safe user store
$localDataDir = __DIR__ . '/data';
if (!file_exists($localDataDir)) {
    @mkdir($localDataDir, 0777, true);
}
$usersFile = $localDataDir . '/users.json';
$tempUsersFile = sys_get_temp_dir() . '/viral_users.json';

// Admin Account Constants
define('ADMIN_EMAIL', 'pallanti2020@gmail.com');
define('ADMIN_PASS', 'Abhi@9246830084');

// Default admin account
$defaultUsers = [
    ADMIN_EMAIL => [
        'id' => 1,
        'name' => 'Pallanti Admin',
        'email' => ADMIN_EMAIL,
        'password' => ADMIN_PASS,
        'role' => 'admin',
        'status' => 'approved',
        'created_at' => '2026-10-29 00:00:00'
    ]
];

function getUsers() {
    global $usersFile, $tempUsersFile, $defaultUsers;
    $content = false;
    if (file_exists($usersFile)) {
        $content = @file_get_contents($usersFile);
    } elseif (file_exists($tempUsersFile)) {
        $content = @file_get_contents($tempUsersFile);
    }
    $users = json_decode($content, true);
    if (!is_array($users)) {
        $users = [];
    }
    // Ensure admin always exists and is approved with latest password
    $users[ADMIN_EMAIL] = $defaultUsers[ADMIN_EMAIL];
    return $users;
}

function saveUsers($users) {
    global $usersFile, $tempUsersFile;
    $json = json_encode($users, JSON_PRETTY_PRINT);
    if (!@file_put_contents($usersFile, $json)) {
        @file_put_contents($tempUsersFile, $json);
    }
}

// Request parameters
$action = $_GET['action'] ?? $_POST['action'] ?? '';
$rawInput = file_get_contents('php://input');
$jsonInput = json_decode($rawInput, true) ?: [];
$params = array_merge($_GET, $_POST, $jsonInput);
if (empty($action) && isset($params['action'])) {
    $action = $params['action'];
}

function verifyAdminAuth($params) {
    $token = $params['token'] ?? '';
    $adminEmail = strtolower(trim($params['admin_email'] ?? ''));
    $adminPass = $params['admin_pass'] ?? '';

    // Direct credentials check
    if ($adminEmail === strtolower(ADMIN_EMAIL) && $adminPass === ADMIN_PASS) {
        return true;
    }

    // Token check
    if (!empty($token)) {
        $decoded = base64_decode($token);
        $parts = explode(':', $decoded, 2);
        if (count($parts) === 2) {
            $users = getUsers();
            $admin = $users[ADMIN_EMAIL] ?? null;
            if ($admin && hash_equals(hash('sha256', $admin['email'] . $admin['password']), $parts[1])) {
                return true;
            }
        }
    }
    return false;
}

// ========================================================
// 1. LOGIN
// ========================================================
if ($action === 'login') {
    $email = strtolower(trim($params['email'] ?? ''));
    $password = $params['password'] ?? '';

    if (empty($email) || empty($password)) {
        sendJson(['error' => 'Please provide both email and password.'], 400);
    }

    $users = getUsers();
    $user = $users[$email] ?? null;

    if (!$user) {
        sendJson(['error' => 'Account not found. Please contact admin for access.'], 401);
    }

    // Password verification with bcrypt support and secure fallback
    $storedHash = $user['password_hash'] ?? '';
    $storedPass = $user['password'] ?? '';
    $passMatches = (!empty($storedHash) && password_verify($password, $storedHash))
        || (!empty($storedPass) && $storedPass === $password)
        || (!empty($storedPass) && @password_verify($password, $storedPass));

    if (!$passMatches) {
        sendJson(['error' => 'Incorrect password.'], 401);
    }

    // Check Approval Status
    if ($user['role'] !== 'admin' && ($user['status'] ?? 'pending') !== 'approved') {
        sendJson([
            'error' => 'Access Denied: Your account is pending admin approval. Only admin-approved users can access the portal.',
            'status_code' => 'pending_approval'
        ], 403);
    }

    $passSecret = $user['password_hash'] ?? ($user['password'] ?? '');
    $token = base64_encode($user['id'] . ':' . hash('sha256', $user['email'] . $passSecret));

    sendJson([
        'status' => 'success',
        'message' => 'Login successful',
        'user' => [
            'id' => $user['id'],
            'name' => $user['name'],
            'email' => $user['email'],
            'role' => $user['role'] ?? 'user',
            'status' => $user['status'] ?? 'approved'
        ],
        'token' => $token
    ]);
}

// ========================================================
// 2. REGISTER (Public Registration Blocked by Admin)
// ========================================================
if ($action === 'register') {
    sendJson([
        'error' => 'Public registration is disabled. Accounts can only be created directly by the Administrator. Please contact pallanti2020@gmail.com for access.',
        'status_code' => 'registration_disabled'
    ], 403);
}

// ========================================================
// 3. ADMIN: GET ALL USERS
// ========================================================
if ($action === 'admin_get_users') {
    if (!verifyAdminAuth($params)) {
        sendJson(['error' => 'Unauthorized: Admin access required.'], 403);
    }

    $users = getUsers();
    $list = [];
    foreach ($users as $u) {
        $list[] = [
            'id' => $u['id'],
            'name' => $u['name'],
            'email' => $u['email'],
            'role' => $u['role'] ?? 'user',
            'status' => $u['status'] ?? 'pending',
            'created_at' => $u['created_at'] ?? 'N/A'
        ];
    }

    sendJson([
        'status' => 'success',
        'users' => $list
    ]);
}

// ========================================================
// 4. ADMIN: TOGGLE USER APPROVAL (Approve / Block)
// ========================================================
if ($action === 'admin_set_status') {
    if (!verifyAdminAuth($params)) {
        sendJson(['error' => 'Unauthorized: Admin access required.'], 403);
    }

    $targetEmail = strtolower(trim($params['email'] ?? ''));
    $newStatus = trim($params['status'] ?? 'approved'); // 'approved' or 'blocked'

    if ($targetEmail === strtolower(ADMIN_EMAIL)) {
        sendJson(['error' => 'Cannot change status of primary admin account.'], 400);
    }

    $users = getUsers();
    if (!isset($users[$targetEmail])) {
        sendJson(['error' => 'User not found.'], 404);
    }

    $users[$targetEmail]['status'] = $newStatus;
    saveUsers($users);

    sendJson([
        'status' => 'success',
        'message' => "User {$targetEmail} is now {$newStatus}."
    ]);
}

// ========================================================
// 5. ADMIN: ADD PRE-APPROVED USER DIRECTLY
// ========================================================
if ($action === 'admin_add_user') {
    if (!verifyAdminAuth($params)) {
        sendJson(['error' => 'Unauthorized: Admin access required.'], 403);
    }

    $name = trim($params['name'] ?? 'Creator');
    $email = strtolower(trim($params['email'] ?? ''));
    $password = $params['password'] ?? 'viral123';
    $status = trim($params['status'] ?? 'approved');

    if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        sendJson(['error' => 'Invalid email address.'], 400);
    }

    $users = getUsers();
    $users[$email] = [
        'id' => time(),
        'name' => $name,
        'email' => $email,
        'password_hash' => password_hash($password, PASSWORD_BCRYPT),
        'role' => 'user',
        'status' => $status,
        'created_at' => date('Y-m-d H:i:s')
    ];
    saveUsers($users);

    sendJson([
        'status' => 'success',
        'message' => "Pre-approved user {$email} added successfully!"
    ]);
}

// ========================================================
// 6. ADMIN: DELETE USER
// ========================================================
if ($action === 'admin_delete_user') {
    if (!verifyAdminAuth($params)) {
        sendJson(['error' => 'Unauthorized: Admin access required.'], 403);
    }

    $targetEmail = strtolower(trim($params['email'] ?? ''));
    if ($targetEmail === strtolower(ADMIN_EMAIL)) {
        sendJson(['error' => 'Cannot delete primary admin account.'], 400);
    }

    $users = getUsers();
    if (isset($users[$targetEmail])) {
        unset($users[$targetEmail]);
        saveUsers($users);
    }

    sendJson([
        'status' => 'success',
        'message' => "User {$targetEmail} removed."
    ]);
}

// ========================================================
// 7. ADMIN: GET & UPDATE SYSTEM SETTINGS (Gemini API Key)
// ========================================================
if ($action === 'admin_get_settings') {
    if (!verifyAdminAuth($params)) {
        sendJson(['error' => 'Unauthorized: Admin access required.'], 403);
    }
    $settings = getSystemSettings();
    sendJson([
        'status' => 'success',
        'settings' => $settings
    ]);
}

if ($action === 'admin_update_settings') {
    if (!verifyAdminAuth($params)) {
        sendJson(['error' => 'Unauthorized: Admin access required.'], 403);
    }

    $geminiKey = trim($params['gemini_api_key'] ?? '');
    if (!empty($geminiKey)) {
        saveSystemSettings(['gemini_api_key' => $geminiKey]);
    }

    sendJson([
        'status' => 'success',
        'message' => 'Admin system settings updated successfully.'
    ]);
}

sendJson(['error' => 'Invalid action.'], 400);
