import json
import os
import sys

import requests


def main() -> int:
    api_key = os.environ.get("VERILEARN_AI_API_KEY", "").strip()
    base_url = os.environ.get("VERILEARN_AI_BASE_URL", "https://api.deepseek.com").strip()
    model = os.environ.get("VERILEARN_AI_MODEL", "deepseek-chat").strip()

    if not api_key:
        print(json.dumps({"ok": False, "error": "VERILEARN_AI_API_KEY is not configured"}, ensure_ascii=False))
        return 1

    response = requests.post(
        base_url.rstrip("/") + "/chat/completions",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        json={
            "model": model,
            "messages": [
                {"role": "system", "content": "You are a concise learning material generator."},
                {
                    "role": "user",
                    "content": "请用中文生成一段约200字的 Markdown 学习材料，主题是：Spring Boot Controller。要求包含标题、概念说明、最小例子。只输出 Markdown 正文。",
                },
            ],
            "temperature": 0.3,
        },
        timeout=60,
    )
    response.raise_for_status()
    data = response.json()
    content = data["choices"][0]["message"]["content"]
    print(json.dumps({
        "ok": True,
        "model": data.get("model"),
        "content_preview": content[:500],
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
