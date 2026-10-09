# External test data: sources, licenses, and what we changed

These files are **test data only**. They are not used to train anything, and they do not ship in the app.

| File | Source | Author | License | Version used |
|---|---|---|---|---|
| `ph-sms-onzero0.json` | [Tagalog-SMS (Kaggle)](https://www.kaggle.com/datasets/onzero0/tagalog-sms) | onzero0 | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) (public domain) | Kaggle version 1 (2025-04-14) |
| `ph-sms-scottleechua.json` | [spam and marketing SMS](https://github.com/scottleechua/data/tree/main/spam-and-marketing-sms) (also on [Kaggle](https://www.kaggle.com/datasets/scottleechua/ph-spam-marketing-sms-w-timestamps)) | Scott Lee Chua | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) | commit `ad50c348` (2026-08-20) |

**Attribution (CC BY 4.0):** "spam and marketing SMS" by Scott Lee Chua, licensed under CC BY 4.0, https://github.com/scottleechua/data. **Changes made:** see below. The original author does not endorse SecondLook.

The CC0 dataset needs no credit; we credit it anyway.

## What we changed (both files)

`scripts/build-external-tests.mjs` downloads the pinned versions above and writes these files. It does the following:

- **Keeps only the message text and category.** The sender column (phone numbers) and timestamps are dropped.
- **Replaces every digit with a random digit.** Lengths stay the same. Dates and times get random but real values. The `09` / `639` prefix of PH mobile numbers is kept, and so is the Luhn validity of card-length numbers, so the detector behaves the same as on the original text. No real OTP, PIN, account, or phone number survives. The random digits come from a seed made of the digit-free text, so the original digits cannot be recovered.
- **Replaces emails** with `someone@example.com`, **links and domains** with `example.invalid/link`, and **first names after a greeting** with `Juan`. The datasets had already replaced most names with `<NAME>`.
- **Removes near-duplicates** (the same text once digits are hidden).
- **Labels:** each file starts from the dataset's own category (`otp` = must warn; `notifs` / `ads` / `gov` / `spam` = must not raise a red warning). A short, reviewed list of corrections is in `OVERRIDES` in the script, each with its reason. Examples: an OTP filed under `notifs`, or a `#BDOStopScam` advisory with no code filed under `otp`.
- In scottleechua's data, the `OTP` and `notifs` texts are `<REDACTED>` at the source, so only `spam`, `ads`, and `gov` are used.

## Privacy (RA 10173, Data Privacy Act of 2012)

Both datasets are messages their authors **received on their own phones and chose to publish**. Most personal details were already redacted by the authors. We remove what is left (numbers, emails, links, names) before anything is committed. Raw downloads are never written to the repo; the script works in memory.

## Other datasets we checked

See the team note `dataset-license-checklist.md`. Datasets with **no license**, a **non-commercial** license, or custom terms (e.g. ai4privacy) are **not** used here.
