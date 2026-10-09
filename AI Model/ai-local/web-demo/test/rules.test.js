// Every synthetic case in test-data/samples.json, run through the browser rules (twin of RuleDetector.kt).
// Rules only: jvm-check scores the same file with the PII model as well.
import { describe, expect, test } from 'vitest';
import { detectRules } from '../public/rules.js';
import { category, readJson } from './helpers.js';

const samples = readJson('samples.json').detect;

describe('samples.json (rules only)', () => {
  test.each(samples.map((s) => [s.expect, s.text]))('%s  <-  %s', (want, text) => {
    expect(category(detectRules(text))).toBe(want);
  });
});

describe('rules basics', () => {
  test('blank text never warns', () => {
    expect(detectRules('')).toBeNull();
    expect(detectRules('   \n ')).toBeNull();
  });

  test('a soft warning stays soft', () => {
    expect(detectRules('Send money to 09171234567')).toMatchObject({ category: 'OTHER_PII', severity: 'MEDIUM' });
  });

  test('the warned span is the secret itself', () => {
    const text = '482913 is your GRAB verification code';
    const r = detectRules(text);
    expect(text.slice(r.start, r.end)).toBe('482913');
  });
});
