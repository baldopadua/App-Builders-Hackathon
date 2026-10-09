import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

// core.js imports transformers.js from the demo server's /vendor path. The tests never load a model, so that import
// is pointed at a stub: rules, Itago and the scoring all run in plain Node, no server, no browser, no model files.
export default defineConfig({
  resolve: {
    alias: [
      { find: '/vendor/transformers/transformers.min.js', replacement: fileURLToPath(new URL('./test/stubs/transformers.js', import.meta.url)) },
    ],
  },
  test: {
    include: ['test/**/*.test.js'],
  },
});
