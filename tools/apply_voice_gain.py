#!/usr/bin/env python3
"""One-off +6.02 dB pre-gain for the 65 Mission Control voice OGGs.

Android SoundPool caps per-stream volume at 1.0, so a 2x voice control cannot be
implemented with SoundPool alone.  This script rewrites only app/res/raw/voice_*.ogg
with 2x gain and a look-ahead limiter, while keeping the original files in
build/voice-original/ for an idempotent rerun.

Requires: numpy, soundfile, imageio-ffmpeg (see tools/requirements-audio.txt).
Not part of the APK or the normal build.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path

import imageio_ffmpeg
import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "app" / "res" / "raw"
BACKUP = ROOT / "build" / "voice-original"
STAGE = ROOT / "build" / "voice-gained"
REPORT = ROOT / "build" / "voice-gain-report.txt"
EXPECTED = 65
GAIN = 2.0
LIMIT = 0.891
PEAK_TOLERANCE = 0.02
DURATION_TOLERANCE = 0.05


def fail(message: str) -> "NoReturn":
    print("ERROR: " + message, file=sys.stderr)
    raise SystemExit(1)


def voice_files() -> list[Path]:
    return sorted(RAW.glob("voice_*.ogg"))


def ensure_backup(files: list[Path]) -> None:
    BACKUP.mkdir(parents=True, exist_ok=True)
    for src in files:
        dst = BACKUP / src.name
        if not dst.exists():
            shutil.copy2(src, dst)
    backups = sorted(BACKUP.glob("voice_*.ogg"))
    if len(backups) != EXPECTED:
        fail(f"expected {EXPECTED} originals in {BACKUP}, found {len(backups)}")


def decode_info(path: Path) -> sf._SoundFileInfo:
    return sf.info(str(path))


def peak_rms(path: Path) -> tuple[float, float]:
    data, _ = sf.read(str(path), dtype="float32", always_2d=True)
    if data.size == 0:
        return 0.0, 0.0
    return float(np.max(np.abs(data))), float(np.sqrt(np.mean(np.square(data))))


def encode(ffmpeg: str, src: Path, dst: Path, sample_rate: int) -> None:
    af = (
        f"volume={GAIN},"
        f"alimiter=limit={LIMIT}:attack=5:release=50:level=false"
    )
    args = [
        ffmpeg, "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
        "-i", str(src),
        "-af", af,
        # q10 keeps the source's ~500 kbps stereo detail; lower quality
        # settings audibly soften the Mission Control voices.
        "-c:a", "libvorbis", "-q:a", "10",
        "-ar", str(sample_rate), "-ac", "2",
        str(dst),
    ]
    proc = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if proc.returncode != 0:
        detail = proc.stderr.decode("utf-8", "replace").strip()
        fail(f"ffmpeg failed for {src.name}: {detail}")


def main() -> None:
    files = voice_files()
    if len(files) != EXPECTED:
        fail(f"expected {EXPECTED} voice files under {RAW}, found {len(files)}")
    ensure_backup(files)

    if STAGE.exists():
        shutil.rmtree(STAGE)
    STAGE.mkdir(parents=True)

    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    report: list[str] = []
    stage: list[Path] = []

    for src in files:
        original = BACKUP / src.name
        info = decode_info(original)
        if info.channels != 2:
            fail(f"{original.name}: expected stereo, found {info.channels} channel(s)")
        if info.samplerate not in (44100, 48000):
            fail(f"{original.name}: unexpected sample rate {info.samplerate}")
        dst = STAGE / src.name
        encode(ffmpeg, original, dst, info.samplerate)

        out_info = decode_info(dst)
        if out_info.channels != info.channels:
            fail(f"{dst.name}: channel count changed")
        if out_info.samplerate != info.samplerate:
            fail(f"{dst.name}: sample rate changed")
        if abs(out_info.duration - info.duration) > DURATION_TOLERANCE:
            fail(f"{dst.name}: duration changed {info.duration} -> {out_info.duration}")
        before_peak, before_rms = peak_rms(original)
        after_peak, after_rms = peak_rms(dst)
        if after_peak > LIMIT + PEAK_TOLERANCE:
            fail(f"{dst.name}: peak {after_peak:.4f} exceeds limiter budget")
        ratio = after_rms / before_rms if before_rms > 0 else 0.0
        report.append(
            f"{src.name}\t{info.duration:.3f}s\t{before_peak:.3f}/{before_rms:.4f}"
            f"\t{after_peak:.3f}/{after_rms:.4f}\tRMS x{ratio:.2f}"
        )
        stage.append(dst)
        print(f"{src.name}: peak {before_peak:.3f}->{after_peak:.3f}, RMS x{ratio:.2f}")

    if len(stage) != EXPECTED:
        fail(f"staged {len(stage)} files, expected {EXPECTED}")

    # All files passed validation, so replace the resource bank as one batch.
    for dst in stage:
        os.replace(dst, RAW / dst.name)

    REPORT.write_text(
        "# voice gain report\n"
        "# file\toriginal duration\toriginal peak/rms\tnew peak/rms\tRMS gain\n"
        + "\n".join(report)
        + "\n",
        encoding="utf-8",
    )
    print(f"OK: rewrote {EXPECTED} voice files; report: {REPORT}")


if __name__ == "__main__":
    main()
