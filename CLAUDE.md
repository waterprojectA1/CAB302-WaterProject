# CAB302 Water Advisory Project — Context for Claude Code

## What this project is
A university group assignment (5 members) building a JavaFX desktop app that
tracks water consumption. My part: **"suggest conservation opportunities
based on recorded data"** — personalized tips, seasonal suggestions,
estimated savings, a conservation score, and links to guides/regulations/
products.

## Tech stack
- Java 21, JavaFX 21.0.12, Maven
- Local LLM inference via **Jlama** (pure-Java, no external server) — used to
  lightly rephrase pre-computed tip sentences, not to generate raw content
- Model: `Qwen2.5-1.5B-Instruct`, quantized to Q4 via Jlama's own quantize
  step (not GGUF/llama.cpp — Jlama uses HF SafeTensors natively)

## Critical design constraint: TipPhraser must never invent facts
`TipPhraser.java` rephrases already-computed sentences (numbers, units,
percentages come from real calculation logic elsewhere). It is NOT a
free-form chat generator. Its system prompt explicitly forbids changing any
numbers. It always falls back to returning the original, unmodified sentence
if generation fails, times out, or looks wrong — never let a UI element
trust model output blindly for anything numeric.

`ChatController`/`ChatView` is a separate, free-form testing scene for
manually probing the model's behavior — it is NOT part of the graded
feature and should stay clearly separated from `TipPhraser`.

## Team folder structure rules (as of the GitHub org migration)
- **`Main.java` must never be modified** — owned by the team, it's the
  app's real entry point (currently loads a login flow via `Arjay_FXML`).
- Every member's Java files go inside their own package folder:
  `src/main/java/com/wateradvisory/<Name>_Root/`
  Mine: `com.wateradvisory.Charlie_Root`
- Every member's FXML files go inside their own resources folder:
  `src/main/resources/<Name>_FXML/`
  Mine: `Charlie_FXML`
- `App_Root.java` / `App_Root-view.fxml` will eventually be the shared main
  menu that routes into each member's feature — **not built yet** (currently
  an empty stub). Don't assume how it will work.
- Because of the above, my two screens (`ConservationTipsView` ↔ `ChatView`)
  navigate between themselves via a self-contained `SceneNavigator` helper
  (swaps the root of whatever `Scene` the clicked button belongs to). This
  does NOT depend on `Main.java` or `App_Root`, so it keeps working
  regardless of how the team wires up the real entry point later.

## How to build and run — READ BEFORE SUGGESTING A RUN COMMAND
The **only** reliable way to run this project is:
```
mvn clean compile exec:exec
```
For previewing my own screens in isolation (without needing `App_Root` or
the login flow finished):
```
mvn clean compile exec:exec@charlie-preview
```
**Do NOT suggest running via VS Code's Run ▶️ button, `mvn javafx:run`, or
launching `Main`/`DevPreviewApp` directly.** All of these break for
documented reasons (see Gotchas below) — the JVM flags this project needs
live in `pom.xml`'s `exec-maven-plugin` config and are only applied when
Maven's `exec:exec` goal is what launches the process.

## Known gotchas (already debugged once — don't rediscover these)
1. **JavaFX classes can't be launched directly on a plain classpath.**
   Any class extending `javafx.application.Application` must be started
   *indirectly* via a plain, non-`Application` launcher class (see
   `Launcher.java` → `Main.java`, and `DevPreviewLauncher.java` →
   `DevPreviewApp.java`). Launching the `Application` subclass directly
   throws `Error: JavaFX runtime components are missing`.
2. **`jdk.incubator.vector` must be added at BOTH compile time and runtime.**
   Compile: `maven-compiler-plugin`'s `--add-modules=jdk.incubator.vector`.
   Runtime: only applied via the `exec-maven-plugin` arguments — this is
   why the run command matters (see above). Missing this throws
   `NoClassDefFoundError: jdk/incubator/vector/FloatVector`.
3. **`jlama-native`'s classifier is `osx-aarch_64`, not `macos-aarch_64`**
   (despite what some blog posts / older docs say) for Apple Silicon.
   Wrong classifier → Maven can't resolve the dependency at all.
4. **`jlama-native` gets silently dropped by JavaFX's automatic
   module-path detection** — `native` is a reserved Java keyword, so its
   derived automatic module name (`jlama.native`) is invalid. This is
   *why* the project uses `exec-maven-plugin` with a plain classpath
   instead of `javafx-maven-plugin`'s `run` goal.
5. **Jlama model folder names use `_` not `/`** on disk, even though
   `jlama list` displays them as `owner/name`. E.g. the registered name
   `Qwen/Qwen2.5-1.5B-Instruct-JQ4` lives at
   `models/Qwen_Qwen2.5-1.5B-Instruct-JQ4`.
6. **`models/` is gitignored** (multi-GB files) — never expect it to be
   present after a fresh clone. `JLAMA_MODEL_HOME` is a per-terminal-session
   env var, not persisted; must be re-exported per session when using the
   `jlama` CLI directly (not needed for the app itself, which uses a
   hardcoded relative path in `ChatController`/`TipPhraser`).
7. **The model has occasionally produced garbled/jumbled output** on a
   repeated prompt after 2-3 turns in the free-form chat tester — cause not
   fully diagnosed (possibly quantization precision, possibly a Jlama
   session-state bug). Treat any model output — especially in
   `TipPhraser` — with suspicion; prefer validating output looks sane
   (mostly alphabetic, right length) before trusting it, and fall back to
   the static template if not.

## Design system (app.css)
Colors/fonts/spacing were extracted directly from a Claude Design mockup
(`Water Advisory Mockups.dc.html`) — not guessed. Uses JavaFX "looked-up
colors" (`-color-*` custom properties on `.root`) to mirror the mockup's
CSS custom properties. Fonts are Caprasimo (headings) + Figtree (body) —
currently falling back to system default since the actual font files
haven't been bundled yet (see comment block at the bottom of `app.css`
for how to add them via `Font.loadFont()`).

## Fallback content
There's a static JSON template bank (`conservation_tip_templates.json`)
with placeholder-based sentence variants per scenario (usage patterns,
seasonal tips, savings framing, impact levels, score bands). This is the
non-AI fallback path and should stay in sync conceptually with whatever
`TipPhraser` produces — if you add a new scenario type to one, consider
whether the other needs it too.
