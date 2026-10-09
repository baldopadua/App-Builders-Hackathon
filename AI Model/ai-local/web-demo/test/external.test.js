// Real Philippine SMS (public datasets, scrubbed: see test-data/external/ATTRIBUTION.md), rules only.
// expect PASSWORD = must warn; expect none = no red (HIGH) warning, a soft "Intended?" is fine.
// Floors and ceilings come from test-data/external/thresholds.json (shared with jvm-check).
import { describe, expect, test } from 'vitest';
import { detectRules } from '../public/rules.js';
import { category, externalSets, readJson } from './helpers.js';

const thresholds = readJson('external/thresholds.json');

describe.each(externalSets())('$file', ({ file, samples }) => {
  const limits = thresholds[file];
  const positives = samples.filter((s) => s.expect !== 'none');
  const negatives = samples.filter((s) => s.expect === 'none');

  test('has thresholds', () => {
    expect(limits, `add ${file} to thresholds.json`).toBeDefined();
  });

  test(`recall on ${positives.length} messages that must warn`, () => {
    const missed = positives.filter((s) => category(detectRules(s.text)) !== s.expect).map((s) => s.text);
    const recall = positives.length ? 1 - missed.length / positives.length : 1;
    expect(recall, `missed:\n${missed.join('\n')}`).toBeGreaterThanOrEqual(limits.minRecall);
  });

  test(`red false alarms on ${negatives.length} messages that must stay quiet`, () => {
    const alarms = negatives
      .map((s) => ({ s, r: detectRules(s.text) }))
      .filter(({ r }) => r?.severity === 'HIGH')
      .map(({ s, r }) => `${r.kind} "${s.text.slice(r.start, r.end)}" | ${s.text}`);
    expect(alarms.length, `red false alarms:\n${alarms.join('\n')}`).toBeLessThanOrEqual(limits.maxRedFalseAlarmsRules);
  });
});
