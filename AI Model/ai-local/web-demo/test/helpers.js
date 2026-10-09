import fs from 'node:fs';

const TEST_DATA = new URL('../../test-data/', import.meta.url);

export const readJson = (name) => JSON.parse(fs.readFileSync(new URL(name, TEST_DATA), 'utf8'));

/** The real PH SMS sets (test-data/external/ph-sms-*.json), scrubbed public datasets. */
export const externalSets = () =>
  fs.readdirSync(new URL('external/', TEST_DATA))
    .filter((f) => f.startsWith('ph-sms-') && f.endsWith('.json'))
    .sort()
    .map((file) => ({ file, samples: readJson(`external/${file}`).detect }));

export const category = (result) => result?.category ?? 'none';
