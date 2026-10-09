// "Itago" (hide it): after masking, the same draft must not warn again. Twin check of jvm-check section 2b.
import { describe, expect, test } from 'vitest';
import { hide, hideAll } from '../public/core.js';
import { detectRules } from '../public/rules.js';
import { externalSets, readJson } from './helpers.js';

const risky = [
  ...readJson('samples.json').detect.filter((s) => s.expect !== 'none'),
  ...externalSets().flatMap((set) => set.samples.filter((s) => s.expect !== 'none')),
];

describe('Itago leaves nothing to warn about (rules only)', () => {
  test.each(risky.map((s) => [s.text]))('%s', async (text) => {
    const masked = await hideAll(text, detectRules);
    expect(detectRules(masked)).toBeNull();
    expect(masked).not.toBe(text);
  });
});

describe('Itago masking shapes', () => {
  test('OTP becomes dots', () => {
    const text = '482913 is your GRAB verification code';
    expect(hide(text, detectRules(text))).toBe('•••••• is your GRAB verification code');
  });

  test('card keeps the last 4 digits', () => {
    const text = 'card ko 4532 0151 1283 0366';
    expect(hide(text, detectRules(text))).toBe('card ko •••• •••• •••• 0366');
  });
});
