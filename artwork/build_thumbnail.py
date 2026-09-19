from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont
import numpy as np
import json
from functools import lru_cache
from shutil import copyfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artwork"
CAPTURES = ROOT / "build/run/thumbnailStudio/screenshots"
OUT.mkdir(exist_ok=True)
(OUT / "frames").mkdir(exist_ok=True)
(OUT / "cutouts").mkdir(exist_ok=True)
(OUT / "audit").mkdir(exist_ok=True)

sprites = []
for path in sorted(CAPTURES.glob("[0-9][0-9]-*.png")):
    rgb = np.array(Image.open(path).convert("RGB"))
    key = (rgb[:, :, 0] > 250) & (rgb[:, :, 1] < 5) & (rgb[:, :, 2] > 250)
    assert key[0, :].all() and key[-1, :].all() and key[:, 0].all() and key[:, -1].all(), "No model or arrow may touch the capture border"
    lit = np.array(ImageEnhance.Brightness(Image.fromarray(rgb)).enhance(1.14))
    rgba = np.dstack([lit, np.where(key, 0, 255).astype(np.uint8)])
    # Clear transparent RGB to prevent magenta fringes during resampling.
    rgba[key, :3] = 0
    cutout = Image.fromarray(rgba)
    cutout.save(OUT / "cutouts" / path.name)
    sprites.append((path.stem, cutout))
assert len(sprites) == 12
boxes = [im.getbbox() for _, im in sprites]
union = (min(b[0] for b in boxes), min(b[1] for b in boxes), max(b[2] for b in boxes), max(b[3] for b in boxes))
centroids = []
for _, sprite in sprites:
    a = np.asarray(sprite.crop(union).getchannel("A"), dtype=float)
    centroids.append(float((a.sum(axis=0) * np.arange(a.shape[1])).sum() / a.sum()))
# Share the alignment so variants don't jump sideways.
visual_center = float(np.mean(centroids))
BACKGROUND_EDGE = np.array([45, 30, 60.])
BACKGROUND_LIGHT = np.array([153, 128, 170.])
BACKGROUND_PALETTE = np.rint(np.linspace(BACKGROUND_EDGE, BACKGROUND_LIGHT, 64)).astype(np.uint8)

@lru_cache(maxsize=3)
def background_glow(size):
    yy, xx = np.mgrid[0:size, 0:size].astype(float) / (size - 1)
    return np.exp(-(((xx - .48) / .64) ** 2 + ((yy - .40) / .72) ** 2) * 1.7)

def composite(sprite, size, indexed=False):
    cropped = sprite.crop(union)
    factor = min(.82 * size / cropped.height, .80 * size / cropped.width)
    scaled = cropped.resize((round(cropped.width * factor), round(cropped.height * factor)), Image.Resampling.LANCZOS)
    x, y = round(size * .49 - visual_center * factor), round(size * .09)
    alpha = np.array(scaled.getchannel("A"))
    shadow = Image.new("L", (size, size))
    draw = ImageDraw.Draw(shadow)
    # The feet sit farther back than the door and need their own contact shadow.
    for column in range(alpha.shape[1]):
        occupied = np.flatnonzero(alpha[:, column] > 128)
        if len(occupied) > alpha.shape[0] * .30 and occupied[-1] > alpha.shape[0] * .68:
            base = y + int(occupied[-1])
            draw.rectangle((x + column, base - size * .006, x + column + 1, base + size * .026), fill=125)
    shadow_alpha = np.asarray(shadow.filter(ImageFilter.GaussianBlur(size * .015)), dtype=float) / 255
    # A shared color ramp keeps the background stable across GIF palettes.
    glow = background_glow(size) * (1 - shadow_alpha)
    colors = BACKGROUND_EDGE + glow[:, :, None] * (BACKGROUND_LIGHT - BACKGROUND_EDGE)
    canvas = Image.fromarray(np.rint(colors).astype(np.uint8)).convert("RGBA")
    overlay = Image.new("RGBA", (size, size))
    overlay.alpha_composite(scaled, (x, y))
    canvas = Image.alpha_composite(canvas, overlay).convert("RGB")
    if not indexed:
        return canvas

    # Dither only the background; keep the model textures crisp.
    bayer = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) + .5) / 16
    thresholds = np.tile(bayer, (int(np.ceil(size / 4)), int(np.ceil(size / 4))))[:size, :size]
    indices = np.clip(np.floor(glow * 63 + thresholds), 0, 63).astype(np.uint8)
    mask = np.asarray(overlay.getchannel("A")) > 0
    pixels = np.asarray(canvas)[mask]
    model = Image.fromarray(pixels[None, :, :]).quantize(colors=192, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    indices[mask] = np.asarray(model)[0] + 64
    result = Image.fromarray(indices).convert("P")
    result.putpalette(BACKGROUND_PALETTE.flatten().tolist() + model.getpalette()[:192 * 3])
    return result

frames = [composite(im, 768) for _, im in sprites]
for (name, _), frame in zip(sprites, frames):
    frame.save(OUT / "frames" / (name + ".png"))

indexed = [composite(im, 768, indexed=True) for _, im in sprites]
gif_path = OUT / "zombie-doors-thumbnail.gif"
indexed[0].save(gif_path, save_all=True, append_images=indexed[1:], duration=850, loop=0, disposal=1, optimize=False)
small = [composite(im, 512, indexed=True) for _, im in sprites]
small[0].save(OUT / "zombie-doors-thumbnail-512.gif", save_all=True, append_images=small[1:], duration=850, loop=0, disposal=1, optimize=False)
composite(sprites[0][1], 1024).save(OUT / "zombie-doors-cover.png")

font = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 17)
contact = Image.new("RGB", (320 * 4, 350 * 3), "#142838")
draw = ImageDraw.Draw(contact)
for i, ((name, _), frame) in enumerate(zip(sprites, frames)):
    col, row = i % 4 * 320, i // 4 * 350
    contact.paste(frame.resize((320, 320), Image.Resampling.LANCZOS), (col, row))
    draw.text((col + 12, row + 326), name[3:].replace("-", " "), font=font, fill="#e1edf3")
contact.save(OUT / "audit-contact-sheet.png")

decoded = Image.open(gif_path)
assert decoded.n_frames == 12 and decoded.info["loop"] == 0 and decoded.size == (768, 768)
decoded_contact = Image.new("RGB", (256 * 4, 280 * 3), "#21192D")
decoded_draw = ImageDraw.Draw(decoded_contact)
background_reference = np.asarray(indexed[0].convert("RGB"))[:, :64].copy()
durations = []
decoded_signatures = set()
for i in range(decoded.n_frames):
    decoded.seek(i)
    rgb = decoded.convert("RGB")
    decoded_signatures.add(rgb.tobytes())
    assert np.array_equal(np.asarray(rgb), np.asarray(indexed[i].convert("RGB"))), "Saved GIF must exactly preserve indexed frames"
    assert np.array_equal(np.asarray(rgb)[:, :64], background_reference), "Backdrop must not flicker between palettes"
    durations.append(decoded.info["duration"])
    assert decoded.info["duration"] == 850
    col, row = i % 4 * 256, i // 4 * 280
    decoded_contact.paste(rgb.resize((256, 256), Image.Resampling.LANCZOS), (col, row))
    decoded_draw.text((col + 12, row + 259), f"GIF frame {i+1:02}", font=font, fill="#e1edf3")
    if i in [0, 2, 7, 11]:
        decoded.convert("RGB").save(OUT / f"audit-gif-frame-{i+1:02}.png")
decoded_contact.save(OUT / "audit/final-decoded-gif-contact-sheet.png")
assert len(decoded_signatures) == 12
small_decoded = Image.open(OUT / "zombie-doors-thumbnail-512.gif")
assert small_decoded.n_frames == 12 and small_decoded.size == (512, 512) and small_decoded.info["loop"] == 0
for i in range(12):
    small_decoded.seek(i)
    assert small_decoded.info["duration"] == 850
    assert np.array_equal(np.asarray(small_decoded.convert("RGB")), np.asarray(small[i].convert("RGB")))
copyfile(gif_path, OUT / "zombie-doors-thumbnail-v2.gif")
copyfile(OUT / "zombie-doors-thumbnail-512.gif", OUT / "zombie-doors-thumbnail-512-v2.gif")
copyfile(OUT / "zombie-doors-cover.png", OUT / "zombie-doors-cover-v2.png")
(OUT / "capture-manifest.json").write_text(json.dumps({
    "source": "Minecraft 26.1.2 actual entity renderer with Zombie Doors 0.1.0",
    "angle_degrees": 40, "elevation_degrees": 25, "image_generation_used": False,
    "capture_size": [1536, 1536], "gif_size": [768, 768],
    "variants": [name for name, _ in sprites], "frame_duration_ms": durations,
    "loop_duration_seconds": sum(durations) / 1000, "bytes": gif_path.stat().st_size,
    "small_gif_bytes": (OUT / "zombie-doors-thumbnail-512.gif").stat().st_size,
    "background": "Dusk violet with a soft central spotlight and vignette",
    "horizontal_alignment": "Average alpha mass centered at 49% canvas width; identical translation across variants",
    "horizontal_shift_pixels_at_768": round((union[2] - union[0]) / 2 * (.82 * 768 / (union[3] - union[1])) - visual_center * (.82 * 768 / (union[3] - union[1])) - .01 * 768, 1),
    "processing": "Exact chroma-key removal, 14% uniform exposure lift, shared crop and optical centering, contact shadow, fixed 64-color background palette plus 192 model colors per frame"
}, indent=2) + "\n")
print(json.dumps({"gif": str(gif_path), "bytes": gif_path.stat().st_size, "frames": decoded.n_frames, "union": union}))
