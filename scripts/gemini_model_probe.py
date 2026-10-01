# Sends the app's scan and chronicle requests to every model GEMINI_API_KEY lists and prints which ones work.
# Run after Google changes its model line-up; update SPECIALISED in GeminiModels.kt from the result.
import os, json, base64, io, time, urllib.request, urllib.error, sys
from PIL import Image, ImageDraw, ImageFont
KEY=os.environ["GEMINI_API_KEY"]; BASE="https://generativelanguage.googleapis.com/v1beta/"
def call(path, body=None, timeout=90):
    req=urllib.request.Request(BASE+path, data=json.dumps(body).encode() if body else None,
        headers={"x-goog-api-key":KEY,"Content-Type":"application/json"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r: return r.status, r.read().decode()
    except urllib.error.HTTPError as e: return e.code, e.read().decode()
    except Exception as e: return 0, str(e)
# synthetic score sheet
img=Image.new("RGB",(1000,700),"white"); d=ImageDraw.Draw(img)
try: f=ImageFont.truetype("arial.ttf",40)
except: f=ImageFont.load_default()
rows=[("WINGSPAN","",""),("","Martin","Shan"),("Birds","34","41"),("Bonus cards","7","5"),("End-of-round goals","12","9"),("Eggs","15","18"),("Food on cards","3","6"),("Tucked cards","8","2"),("TOTAL","79","81")]
for i,(a,b,c) in enumerate(rows):
    y=30+i*70; d.text((40,y),a,fill="black",font=f); d.text((560,y),b,fill="black",font=f); d.text((780,y),c,fill="black",font=f)
buf=io.BytesIO(); img.save(buf,"JPEG",quality=75); B64=base64.b64encode(buf.getvalue()).decode()
SCAN_PROMPT='''You are a board game score extractor. I will show you a photo of a handwritten or printed score sheet.
Extract all player names and their final scores. Also try to identify the board game this score sheet belongs to.
RULES:
- Return ONLY valid JSON, no preamble, explanation, or markdown fences.
- The JSON must have exactly this shape:
  {"date": "YYYY-MM-DD","players": [{ "name": "Alice", "score": "42", "isWinner": true }],"detectedGameTitle": "Wingspan","detectedGameConfidence": 0.85,"detectedScoringCategories": ["Birds"],"gameDetectionEvidence": "..."}
- If no date is visible return null for date.'''
scan={"contents":[{"parts":[{"text":SCAN_PROMPT},{"inlineData":{"mimeType":"image/jpeg","data":B64}}]}],
      "generationConfig":{"temperature":0.15,"topP":0.9,"maxOutputTokens":2048,"responseMimeType":"application/json"}}
chron={"contents":[{"parts":[{"text":'Write one short chronicle line for a remembered board game session. One sentence, max 120 characters. Return ONLY valid JSON, format exactly: {"chronicleLine":"..."}\nGame: Wingspan\nMoods: Cozy, Tense\nQuote: None\nPlayers: Martin, Shan'}]}],
       "generationConfig":{"temperature":1.0,"topP":0.95,"maxOutputTokens":96,"responseMimeType":"application/json"}}
def text_of(body):
    j=json.loads(body); c=j.get("candidates",[{}])[0]
    t="".join(p.get("text","") for p in c.get("content",{}).get("parts",[]) if not p.get("thought"))
    return t, c.get("finishReason"), j.get("usageMetadata",{}).get("thoughtsTokenCount",0)
def run(model, body, check):
    for attempt in range(3):
        t0=time.time(); code,resp=call(f"models/{model}:generateContent", body); ms=int((time.time()-t0)*1000)
        if code in (503,500,0) and attempt<2: time.sleep(4); continue
        break
    if code!=200:
        try: msg=json.loads(resp)["error"]["message"][:110]
        except: msg=resp[:110]
        return {"code":code,"ok":False,"ms":ms,"note":msg.replace("\n"," ")}
    try:
        t,fin,th=text_of(resp); t=t.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        ok,note=check(json.loads(t))
    except Exception as e: ok,note,fin,th=False,f"parse: {e}"[:80],locals().get("fin"),locals().get("th",0)
    return {"code":200,"ok":ok,"ms":ms,"finish":fin,"thoughts":th,"note":note}
def chk_scan(j):
    p={x["name"].lower():str(x["score"]) for x in j["players"]}
    good=p.get("martin")=="79" and p.get("shan")=="81"
    win=[x["name"] for x in j["players"] if x.get("isWinner")]
    return good and win==["Shan"] and "wingspan" in (j.get("detectedGameTitle") or "").lower(), f"{p} win={win} game={j.get('detectedGameTitle')}"
def chk_chron(j):
    l=j.get("chronicleLine","").strip(); return bool(l), l[:60]
code,resp=call("models?pageSize=1000"); models=[m["name"].removeprefix("models/") for m in json.loads(resp)["models"] if "generateContent" in m.get("supportedGenerationMethods",[])]
out={}
for m in models:
    s=run(m,scan,chk_scan); time.sleep(1.5); c=run(m,chron,chk_chron); time.sleep(1.5)
    out[m]={"scan":s,"chron":c}
    print(f"{m:42} SCAN {s['code']} {'OK ' if s['ok'] else 'BAD'} {s['ms']:6}ms th={s.get('thoughts',0)} {s.get('finish','')} | CHRON {c['code']} {'OK ' if c['ok'] else 'BAD'} {c['ms']:6}ms th={c.get('thoughts',0)} {c.get('finish','')}", flush=True)
    if not s['ok']: print("      scan:",s['note'], flush=True)
    if not c['ok']: print("      chron:",c['note'], flush=True)
