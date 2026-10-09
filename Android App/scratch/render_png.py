import os
import subprocess

svg_path = os.path.abspath('assets/icons/Tanggolin Icon.svg')
out_png = os.path.abspath('assets/icons/tanggolin_icon.png')
html_path = os.path.abspath('assets/icons/render.html')

html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset='utf-8'>
<style>
  html, body {{
    margin: 0;
    padding: 0;
    width: 512px;
    height: 512px;
    background: transparent;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
  }}
  img {{
    width: 460px;
    height: 460px;
    object-fit: contain;
  }}
</style>
</head>
<body>
  <img src='file:///{svg_path.replace(os.sep, "/")}'>
</body>
</html>"""

with open(html_path, 'w', encoding='utf-8') as f:
    f.write(html)

edge_paths = [
    r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Google\Chrome\Application\chrome.exe'
]

browser = next((p for p in edge_paths if os.path.exists(p)), None)
print('Browser:', browser)

url_arg = 'file:///' + html_path.replace(os.sep, '/')
cmd = [
    browser,
    '--headless',
    '--disable-gpu',
    '--default-background-color=00000000',
    '--window-size=512,512',
    f'--screenshot={out_png}',
    url_arg
]
subprocess.run(cmd, check=True)
if os.path.exists(out_png):
    print('Generated PNG successfully size:', os.path.getsize(out_png))
if os.path.exists(html_path):
    os.remove(html_path)
