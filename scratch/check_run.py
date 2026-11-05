import urllib.request
import json
import sys

url = "https://api.github.com/repos/Digitechguide/ViralClip-Studio/actions/runs?per_page=3"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})

try:
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        runs = data.get("workflow_runs", [])
        if not runs:
            print("No runs found")
            sys.exit(0)
        latest = runs[0]
        print(f"ID: {latest['id']}, Name: {latest['name']}, Status: {latest['status']}, Conclusion: {latest['conclusion']}, Head SHA: {latest['head_sha'][:7]}")
except Exception as e:
    print(f"Error checking run: {e}")
