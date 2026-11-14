# Contributing to ARMOR-WATCH

1. **Before a pull request** run `python -I tools/armor_ci_validate.py` and the tests of the project: both must pass.
2. **The README exists in eight languages** (`README.md` and `README_spa.md`, `README_fra.md`, `README_ita.md`, `README_deu.md`, `README_zho.md`, `README_jpn.md`, `README_por.md`). They are generated from the sources in ARMOR-DOCS, so a change to the text is made there and the eight files are regenerated.
3. **Texts that people read** (panels, messages, help) exist in eight languages: English, Spanish, French, Italian, German, Chinese, Japanese and European Portuguese.
4. **A change to a message between the phone and the watch** starts in `docs/watch_alert_contract.json` and comes with accepted and rejected examples; the phone apps keep an identical copy.
5. **Nothing is described as working on a watch** that has only been tested on a computer or an emulator.
6. Commits are written in English, small and about one thing. No secrets in the repository.
