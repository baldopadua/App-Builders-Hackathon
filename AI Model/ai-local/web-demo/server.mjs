// Zero-dependency static server. Serves ONLY local files:
//   /                      -> public/
//   /models/<m>/...        -> ../assets/models/<m>/...   (the same files that go in the APK)
//   /vendor/transformers/  -> node_modules/@huggingface/transformers/dist
//   /vendor/ort/           -> node_modules/onnxruntime-web/dist   (WASM runtime)
// Nothing is fetched from the internet and no request body is logged.
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(here, '..');
const NM = path.join(here, 'node_modules');
const PORT = Number(process.env.PORT ?? 5173);

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.wasm': 'application/wasm',
  '.onnx': 'application/octet-stream', '.txt': 'text/plain; charset=utf-8',
};

function resolve(urlPath) {
  const p = decodeURIComponent(urlPath.split('?')[0]);
  if (p.includes('..')) return null;
  if (p.startsWith('/vendor/transformers/')) return path.join(NM, '@huggingface', 'transformers', 'dist', p.slice(21));
  if (p.startsWith('/vendor/ort/')) return path.join(NM, 'onnxruntime-web', 'dist', p.slice(12));
  if (p.startsWith('/test-data/')) return path.join(ROOT, 'test-data', p.slice(11));
  // Transformers.js looks for <model>/onnx/model_quantized.onnx; our files sit flat in assets/models/<model>/
  // Bundled models live in assets/models/; experimental candidates in lab-models/ (never packaged in the APK).
  const model = (name, rel) => {
    const bundled = path.join(ROOT, 'assets', 'models', name, rel);
    return fs.existsSync(bundled) ? bundled : path.join(ROOT, 'lab-models', name, rel);
  };
  let m = p.match(/^\/models\/([^/]+)\/onnx\/(.+)$/);
  if (m) return model(m[1], m[2]);
  m = p.match(/^\/models\/([^/]+)\/(.+)$/);
  if (m) return model(m[1], m[2]);
  return path.join(here, 'public', p === '/' ? 'index.html' : p);
}

http.createServer((req, res) => {
  const file = resolve(req.url ?? '/');
  if (!file || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
    res.writeHead(404); res.end('not found'); console.log('404', req.url); return;
  }
  res.writeHead(200, {
    'Content-Type': TYPES[path.extname(file)] ?? 'application/octet-stream',
    'Content-Length': fs.statSync(file).size,
    'Cache-Control': 'no-store',
  });
  fs.createReadStream(file).pipe(res);
}).listen(PORT, '127.0.0.1', () => console.log(`SecondLook web demo: http://127.0.0.1:${PORT}`));
