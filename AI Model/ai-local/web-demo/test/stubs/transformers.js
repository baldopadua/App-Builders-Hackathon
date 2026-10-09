// Stand-in for /vendor/transformers/transformers.min.js in Vitest (see vitest.config.js).
// Only what core.js touches at import time; calling a model from a test is a mistake, so it throws.
const noModel = () => { throw new Error('No model in unit tests: test the rules, or run jvm-check / eval.html for the models'); };

export const env = { allowLocalModels: true, allowRemoteModels: false, localModelPath: '', backends: { onnx: { wasm: {} } } };
export const AutoTokenizer = { from_pretrained: noModel };
export const AutoModelForTokenClassification = { from_pretrained: noModel };
export const pipeline = noModel;
