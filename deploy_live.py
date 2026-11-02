import ftplib
import os
import sys
import time

sys.stdout.reconfigure(encoding='utf-8', line_buffering=True)

# Load credentials from .env.deploy if present, or environment variables
def load_env_file(filepath='.env.deploy'):
    env_vars = {}
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    k, v = line.split('=', 1)
                    env_vars[k.strip()] = v.strip()
    return env_vars

_env = load_env_file()
FTP_HOST = os.environ.get('FTP_HOST', _env.get('FTP_HOST', 'ftp.mrpallanti.in'))
FTP_USER = os.environ.get('FTP_USER', _env.get('FTP_USER', 'anti@viral.mrpallanti.in'))
FTP_PASS = os.environ.get('FTP_PASS', _env.get('FTP_PASS', ''))
REMOTE_ROOT = '/viral'

if not FTP_PASS:
    print('Error: FTP_PASS is not set in environment or .env.deploy')
    sys.exit(1)

files_to_upload = [
    ('.htaccess', '.htaccess'),
    ('index.html', 'index.html'),
    ('ViralClip-Studio.apk', 'ViralClip-Studio.apk'),
    ('studio.html', 'studio.html'),
    ('admin.html', 'admin.html'),
    ('css/landing.css', 'css/landing.css'),
    ('css/style.css', 'css/style.css'),
    ('js/app.js', 'js/app.js'),
    ('js/video-engine.js', 'js/video-engine.js'),
    ('js/youtube-helper.js', 'js/youtube-helper.js'),
    ('js/ai-analyzer.js', 'js/ai-analyzer.js'),
    ('api/config.php', 'api/config.php'),
    ('api/auth.php', 'api/auth.php'),
    ('api/suggest.php', 'api/suggest.php'),
    ('api/download.php', 'api/download.php'),
    ('api/info.php', 'api/info.php'),
    ('api/data/.htaccess', 'api/data/.htaccess'),
    ('Viral_Video_Renderer_Colab.ipynb', 'Viral_Video_Renderer_Colab.ipynb')
]

def get_connection():
    ftp = ftplib.FTP(timeout=300)
    ftp.connect(FTP_HOST, 21)
    ftp.login(FTP_USER, FTP_PASS)
    ftp.set_pasv(True)
    ftp.cwd(REMOTE_ROOT)
    return ftp

def upload_all():
    print('Starting deployment to ' + FTP_HOST + '...')
    ftp = get_connection()
    print('Connected. Remote root is: ' + ftp.pwd())

    for local_path, remote_path in files_to_upload:
        if not os.path.exists(local_path):
            print(f'Skip missing: {local_path}')
            continue

        size = os.path.getsize(local_path)
        print(f'Uploading {local_path} -> {remote_path} ({size} bytes)...')

        parts = remote_path.split('/')
        if len(parts) > 1:
            subdirs = parts[:-1]
            target_filename = parts[-1]
            curr_dir = REMOTE_ROOT
            for sd in subdirs:
                curr_dir += '/' + sd
                try:
                    ftp.cwd(curr_dir)
                except Exception:
                    try:
                        ftp.mkd(curr_dir)
                    except Exception:
                        pass
                    ftp.cwd(curr_dir)
        else:
            ftp.cwd(REMOTE_ROOT)
            target_filename = parts[0]

        uploaded = False
        for attempt in range(1, 4):
            try:
                with open(local_path, 'rb') as f:
                    uploaded_bytes = 0
                    def on_chunk(chunk):
                        nonlocal uploaded_bytes
                        uploaded_bytes += len(chunk)
                        if size > 10 * 1024 * 1024 and (uploaded_bytes % (15 * 1024 * 1024) < 1024 * 1024 or uploaded_bytes >= size):
                            pct = int((uploaded_bytes / size) * 100)
                            print(f'    [{pct}%] {uploaded_bytes // (1024*1024)}MB / {size // (1024*1024)}MB')

                    ftp.storbinary(f'STOR {target_filename}', f, blocksize=1024 * 1024, callback=on_chunk)
                print(f'  [OK] Success: {remote_path}')
                uploaded = True
                break
            except Exception as e:
                print(f'  Attempt {attempt} failed: {e}. Reconnecting...')
                time.sleep(2)
                try:
                    ftp.close()
                except Exception:
                    pass
                ftp = get_connection()
                if len(parts) > 1:
                    ftp.cwd(REMOTE_ROOT + '/' + parts[0])
                else:
                    ftp.cwd(REMOTE_ROOT)

        if not uploaded:
            print(f'  [FAIL] Could not upload {remote_path}')

    try:
        ftp.quit()
    except Exception:
        pass
    print('All files successfully uploaded!')

if __name__ == '__main__':
    upload_all()
