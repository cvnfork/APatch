import json
import os
import sys
import html
import urllib.request
import uuid

def get_commit_summary():
    try:
        event_path = os.environ.get("GITHUB_EVENT_PATH")
        with open(event_path, 'r') as f:
            event = json.load(f)
    except Exception:
        return "No commit data available"

    summary_lines = []
    commits = event.get("commits", [])
    
    if commits:
        for c in commits[::-1]:
            msg = html.escape(c.get("message", "").split('\n')[0].strip())
            author = html.escape(c.get("author", {}).get("username", "unknown"))
            summary_lines.append(f"• {msg} by <b>{author}</b>")
            
            if len(summary_lines) >= 10:
                summary_lines.append(f"<i>(...and {len(commits) - 10} more)</i>")
                break
    else:
        head = event.get("head_commit", {})
        msg = html.escape(head.get("message", "Manual Build").split('\n')[0])
        summary_lines.append(f"• {msg}" if msg else "No new commits.")

    return "\n".join(summary_lines)

def send_to_telegram(chat_id, file_path):
    token = os.environ.get("BOT_TOKEN")
    title = html.escape(os.environ.get("TITLE", "Manager"))
    branch = html.escape(os.environ.get("BRANCH", "main"))
    run_url = os.environ.get("RUN_URL", "#")
    
    try:
        with open(os.environ.get("GITHUB_EVENT_PATH"), 'r') as f:
            event = json.load(f)
            compare_url = event.get("compare", run_url)
    except:
        compare_url = run_url

    summary = get_commit_summary()
    
    caption = (
        f"<b>{title}</b>\n"
        f"Branch: <code>{branch}</code>\n\n"
        f"<blockquote>{summary}</blockquote>\n\n"
        f"<a href=\"{compare_url}\">Compare</a> | "
        f"<a href=\"{run_url}\">Workflow</a>"
    )

    media_payload = json.dumps([{
        "type": "document",
        "media": "attach://apk_file",
        "caption": caption[:1024], 
        "parse_mode": "HTML"
    }])

    boundary = f'----APatchBuild-{uuid.uuid4().hex}'
    with open(file_path, 'rb') as f:
        file_content = f.read()

    body = (
        f'--{boundary}\r\nContent-Disposition: form-data; name="chat_id"\r\n\r\n{chat_id}\r\n'
        f'--{boundary}\r\nContent-Disposition: form-data; name="media"\r\n\r\n{media_payload}\r\n'
        f'--{boundary}\r\nContent-Disposition: form-data; name="apk_file"; filename="{os.path.basename(file_path)}"\r\n'
        f'Content-Type: application/vnd.android.package-archive\r\n\r\n'
    ).encode('utf-8') + file_content + f'\r\n--{boundary}--\r\n'.encode('utf-8')

    req = urllib.request.Request(f"https://api.telegram.org/bot{token}/sendMediaGroup", data=body)
    req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
    
    try:
        with urllib.request.urlopen(req) as res:
            print(f"Success: {res.read().decode()}")
    except Exception as e:
        print(f"Error: {e}")
        if hasattr(e, 'read'): print(e.read().decode())
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(1)
    send_to_telegram(sys.argv[1], sys.argv[2])