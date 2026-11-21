# Audio

`audio/` synthesises every cue at runtime - there are no sound files in the jar.

## The engine

A daemon thread owns a `SourceDataLine` and fills it in 512-frame blocks. The game thread only leaves a message on a
queue, because the audio thread has ~11 ms per block and must never wait on a slow tick. **Broken audio is silent
audio**: no device, an exclusive-mode driver or a write failure all set `broken` and stop, and the cutscene plays on.

`SbcsAudio.tick()` runs every client tick, unconditionally - gated behind one animation's check, switching that
animation off left the synth unstarted and every other animation silent.

Measured rules (they will be wrong again if changed carelessly):

- **A bandpass throws away most of white noise's energy**, more so at low Q. `BANDPASS_MAKEUP` puts it back (impacts
  were around -30 dB without it).
- **Normalise harmonics by root-sum-of-squares, not by the plain sum.** Partials only all peak together for an
  instant; dividing by the sum makes every multi-harmonic voice far too quiet.
- **`Voices.after` is sample-accurate.** Rounding a delay to a whole block is 11.6 ms, which splits a 4 ms metallic
  ring into a separate event and makes arpeggios uneven.
- `SynthEngine.softClip` is linear to 0.5, then tanh to 1 (a cubic stepped from 0.83 to 1.0 at full scale).
- A voice must eventually return false, or it accumulates in the engine forever.
- `Voices.gained` is pure, so offline tools can reach cue lists (`HuntSound`, `SeraphSound`) without touching
  `SbcsAudio`.

`SynthEngine.renderOffline` renders a voice to a float array with no device - the way cues are checked: envelope,
peak, DC offset, whether the voice ends.

## Cue lists

Newer cutscenes compute their whole soundtrack once as timed cues (`audio/HellionSound.schedule(tl)`,
`SeraphSound`, `EruptionSound`, `WaterSound`, `VaultSound`, `SlayerSound`, `HuntSound`; the Ossuary's is
event-driven). The screen plays what is due on scene time; a `cut` cue silences first (a reveal, a freeze). Cues more
than `STALE_CUE_MS` late are skipped (a `cut` still applies). At higher animation speeds cues just fire sooner - they
are not pitched or stretched.

**Sound honesty**: every cutscene's test renders all audio before the reveal for every outcome and requires identical
samples. Anything outcome-specific starts at or after the reveal.

Each source has its own `tools/*Soundtrack` that renders the cue list to a WAV - the only way to hear it offline.
They need slf4j on the classpath (SynthEngine logs).

## Silencing the game

`mixin/SoundEngineMixin` refuses the game's own sounds while a cutscene owns the screen, because Hypixel's drop chime
can land before the reveal and give it away. It hooks **both** `play` and `playDelayed`: a chime queued on the kill
tick is a delayed sound. This is a separate path from the synth's own line - silencing one does not silence the other.

## Volumes

Every source has its own volume (`*Volume` in config), applied in `SbcsAudio.play<Source>`, and also exposed on the
menu's Sound tab (`snd_*` slider ids). Win cues on Diana use the separate win volume.
