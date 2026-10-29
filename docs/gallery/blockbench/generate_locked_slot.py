"""Builds a Blockbench model of a Hotbar Locker slot with lock/unlock animations.

Usage: python -I gen_locked_slot.py <vanilla_dir> <out.bbmodel> [<frames_dir>]

The frame/tint/selection layer is a 60 fps flipbook computed with the same math
as LockSlotRenderer; the padlock is real geometry animated on the timeline.
"""
import base64, json, math, os, struct, sys, uuid, zlib

# ---------------------------------------------------------------- PNG I/O
def read_png(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos, idat, palette, trns = 8, b'', None, None
    while pos < len(data):
        length, kind = struct.unpack('>I4s', data[pos:pos + 8])
        chunk = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b'IHDR':
            width, height, depth, ctype, _, _, interlace = struct.unpack('>IIBBBBB', chunk)
            assert interlace == 0 and (depth == 8 or ctype in (0, 3)), (path, depth, interlace)
        elif kind == b'PLTE':
            palette = [tuple(chunk[i:i + 3]) for i in range(0, len(chunk), 3)]
        elif kind == b'tRNS':
            trns = chunk
        elif kind == b'IDAT':
            idat += chunk
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    bpp = max(1, channels * depth // 8)
    stride = (width * channels * depth + 7) // 8
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        f = raw[i]; line = bytearray(raw[i + 1:i + 1 + stride]); i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line); prev = line
    pixels = []
    for line in rows:
        if depth < 8:
            mask, per = (1 << depth) - 1, 8 // depth
            line = bytes((line[x // per] >> (8 - depth * (x % per + 1))) & mask for x in range(width))
            if ctype == 0: line = bytes(v * 255 // mask for v in line)
        out = []
        for x in range(width):
            px = line[x * channels:(x + 1) * channels]
            if ctype == 6: out.append(tuple(px))
            elif ctype == 2: out.append((*px, 255))
            elif ctype == 4: out.append((px[0], px[0], px[0], px[1]))
            elif ctype == 0: out.append((px[0], px[0], px[0], 255))
            else:
                idx = px[0]
                alpha = trns[idx] if trns is not None and idx < len(trns) else 255
                out.append((*palette[idx], alpha))
        pixels.append(out)
    return pixels

def png_bytes(pixels):
    height, width = len(pixels), len(pixels[0])
    raw = b''.join(b'\x00' + bytes(v for px in row for v in px) for row in pixels)
    def chunk(kind, body):
        return struct.pack('>I', len(body)) + kind + body + struct.pack('>I', zlib.crc32(kind + body))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

def data_uri(pixels):
    return 'data:image/png;base64,' + base64.b64encode(png_bytes(pixels)).decode()

def blank(w, h):
    return [[(0, 0, 0, 0)] * w for _ in range(h)]

# ------------------------------------------------- GUI compositing (straight alpha)
def over(dst, x, y, argb_or_rgba):
    if not (0 <= y < len(dst) and 0 <= x < len(dst[0])): return
    r, g, b, a = argb_or_rgba
    if a <= 0: return
    dr, dg, db, da = dst[y][x]
    sa, sda = a / 255, da / 255
    oa = sa + sda * (1 - sa)
    if oa <= 0: dst[y][x] = (0, 0, 0, 0); return
    mix = lambda s, d: round((s * sa + d * sda * (1 - sa)) / oa)
    dst[y][x] = (mix(r, dr), mix(g, dg), mix(b, db), round(oa * 255))

def argb(color):
    return ((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)

def fill(dst, x1, y1, x2, y2, color):
    rgba = argb(color)
    for y in range(y1, y2):
        for x in range(x1, x2):
            over(dst, x, y, rgba)

# ------------------------------------------------- LockSlotRenderer mirror
DURATION, FRAME_END, BADGE_START = 520.0, 380.0, 360.0
ARM_LENGTH = 9 + 17 + 8
FRAME_GOLD, CORNER_GOLD, GLOW_GOLD, TINT_GOLD = 0xF0D9A33D, 0xFFF0C45A, 0xFFFFE9A8, 0x30D9A33D
SEL_W, SEL_H, SEL_BAND = 24, 23, 5
TINT = (1.0, 0.80, 0.40)
SELECTION_FADE = 120.0
OUTLINE, SHACKLE, BODY_LIGHT, BODY, BODY_SHADE = 0xFF3A2306, 0xFFC3C8CF, 0xFFF6D27A, 0xFFE0A93A, 0xFFB07A22
PADLOCK = [".ooooo.", ".o###o.", ".o#o#o.", "ooooooo", "o+++++o", "o==o==o", "o-----o", "ooooooo"]
PADLOCK_COLORS = {'o': OUTLINE, '#': SHACKLE, '+': BODY_LIGHT, '=': BODY, '-': BODY_SHADE}

clamp01 = lambda v: max(0.0, min(1.0, v))
def ease_in_out_cubic(t): return 4 * t ** 3 if t < 0.5 else 1 - (-2 * t + 2) ** 3 / 2
def ease_out_cubic(t): return 1 - (1 - t) ** 3
def ease_out_back(t):
    c1 = 1.70158; c3 = c1 + 1; s = t - 1
    return 1 + c3 * s ** 3 + c1 * s ** 2
def ease_in_quad(t): return t * t

def scale_alpha(color, factor):
    return (round((color >> 24 & 255) * clamp01(factor)) << 24) | (color & 0xFFFFFF)

def lerp_color(a, b, t):
    out = 0
    for shift in range(0, 32, 8):
        x, y = a >> shift & 255, b >> shift & 255
        out |= round(x + (y - x) * t) << shift
    return out

def java_round(v):
    return math.floor(v + 0.5)

def draw_frame(dst, x, y, time, locking):
    progress = ease_in_out_cubic(clamp01(time / FRAME_END))
    fill(dst, x + 2, y + 2, x + 18, y + 18, scale_alpha(TINT_GOLD, progress))
    color = FRAME_GOLD
    if locking and time > FRAME_END:
        glow = 1 - clamp01((time - FRAME_END) / (DURATION - FRAME_END))
        color = lerp_color(FRAME_GOLD, GLOW_GOLD, glow * glow)
    if progress >= 1:
        fill(dst, x + 1, y + 1, x + 19, y + 2, color)
        fill(dst, x + 1, y + 18, x + 19, y + 19, color)
        fill(dst, x + 1, y + 2, x + 2, y + 18, color)
        fill(dst, x + 18, y + 2, x + 19, y + 18, color)
        corner = CORNER_GOLD if color == FRAME_GOLD else color
        for cx, cy in ((1, 1), (18, 1), (1, 18), (18, 18)):
            fill(dst, x + cx, y + cy, x + cx + 1, y + cy + 1, corner)
        return
    length = progress * ARM_LENGTH
    for step in range(ARM_LENGTH):
        coverage = clamp01(length - step)
        if coverage <= 0: break
        if step < 9: px, py = 9 - step, 18
        elif step < 26: px, py = 1, 17 - (step - 9)
        else: px, py = 2 + (step - 26), 1
        pixel = scale_alpha(color, coverage)
        fill(dst, x + px, y + py, x + px + 1, y + py + 1, pixel)
        fill(dst, x + 19 - px, y + py, x + 20 - px, y + py + 1, pixel)

def draw_selection(dst, sprite, x, y, gold, clip=None):
    mul = [1 + (t - 1) * gold for t in TINT]
    for sy, row in enumerate(sprite):
        for sx, (r, g, b, a) in enumerate(row):
            if clip is not None and not any(fx <= sx < tx and fy <= sy < ty for fx, fy, tx, ty in clip): continue
            over(dst, x + sx, y + sy, (round(r * mul[0]), round(g * mul[1]), round(b * mul[2]), a))

def trace_clips(progress):
    half = SEL_W // 2
    length = progress * (half + SEL_H + half)
    bottom = java_round(min(length, half))
    side = java_round(max(0.0, min(SEL_H, length - half)))
    across = java_round(max(0.0, min(half, length - half - SEL_H)))
    rects = [(half - bottom, SEL_H - SEL_BAND, half + bottom, SEL_H),
             (0, SEL_H - side, SEL_BAND, SEL_H),
             (SEL_W - SEL_BAND, SEL_H - side, SEL_W, SEL_H),
             (0, 0, across, SEL_BAND),
             (SEL_W - across, 0, SEL_W, SEL_BAND)]
    return [r for r in rects if r[2] > r[0] and r[3] > r[1]]

# ------------------------------------------------- Scene layout (canvas pixels)
CANVAS = 24              # 24x24: selection sprite at rows 1..23, badge top at row 0
SLOT = (2, 3)            # 20x20 slot origin
SELECTION = (0, 1)       # vanilla selection sprite origin (slot - 2)
ITEM = (4, 5)            # 16x16 item origin (slot + 2)
BADGE = (1, 0)           # padlock origin (slot + (-1, -3))
HOTBAR_SLOT = 1          # which hotbar slot's background to crop
FPS = 60
LENGTH = 0.6             # seconds; last frame holds the end state
FRAMES = int(LENGTH * FPS) + 1
# Blockbench wraps flipbooks by frame count, so the end state is repeated long
# enough to cover scrubbing or holding past the animation's end.
HOLD_SECONDS = 10

def fx_frame(sprite, ms, locking):
    """Everything vanilla draws below the item for this slot: our frame/tint and the selection."""
    dst = blank(CANVAS, CANVAS)
    elapsed = min(DURATION, ms)
    time = elapsed if locking else DURATION - elapsed
    if time > 0:
        draw_frame(dst, *SLOT, time, locking)
    if locking:
        tracing = time < FRAME_END
        draw_selection(dst, sprite, *SELECTION, 0.0 if tracing else 1.0)
        if tracing:
            progress = ease_in_out_cubic(clamp01(time / FRAME_END))
            draw_selection(dst, sprite, *SELECTION, 1.0, trace_clips(progress))
    else:
        draw_selection(dst, sprite, *SELECTION, max(0.0, 1 - ms / SELECTION_FADE))
    return dst

def badge_state(ms, locking):
    """(visible scale, shackle lift) for the padlock; alpha is folded into scale."""
    elapsed = min(DURATION, ms)
    time = elapsed if locking else DURATION - elapsed
    if time <= BADGE_START: return 0.0, 2.0
    p = (time - BADGE_START) / (DURATION - BADGE_START)
    alpha = ease_out_cubic(p)
    scale = (0.6 + 0.4 * ease_out_back(p)) * alpha
    lift = 2 * (1 - ease_in_quad(clamp01((p - 0.4) / 0.6)))
    return scale, lift

# ------------------------------------------------- Blockbench model
def uid(): return str(uuid.uuid4())
def to_bb(cx, cy): return [cx - CANVAS / 2, CANVAS / 2 - cy]   # canvas px -> BB units (y up)

def plane(name, cx, cy, w, h, z, texture, uv):
    x1, y2 = to_bb(cx, cy); x2, y1 = to_bb(cx + w, cy + h)
    none = {"uv": [0, 0, 0, 0], "texture": None}
    return {"name": name, "box_uv": False, "rescale": False, "locked": False, "light_emission": 0,
            "render_order": "default", "allow_mirror_modeling": True,
            "from": [x1, y1, z], "to": [x2, y2, z], "autouv": 0, "color": 0, "origin": [0, 0, 0],
            "faces": {"north": dict(none), "east": dict(none), "south": {"uv": uv, "texture": texture},
                      "west": dict(none), "up": dict(none), "down": dict(none)},
            "type": "cube", "uuid": uid()}

def group(name, origin, children):
    return {"name": name, "origin": origin, "color": 0, "uuid": uid(), "export": True, "mirror_uv": False,
            "isOpen": True, "locked": False, "visibility": True, "autouv": 0, "children": children}

def texture(name, pixels, uv_w, uv_h, tid, translucent=False, fps=7):
    return {"path": "", "name": name, "folder": "", "namespace": "", "id": str(tid), "group": "",
            "width": len(pixels[0]), "height": len(pixels), "uv_width": uv_w, "uv_height": uv_h,
            "particle": False, "use_as_default": False, "layers_enabled": False, "sync_to_project": "",
            "render_mode": "default", "render_sides": "auto", "fps": fps, "force_translucent": translucent,
            "frame_time": 1, "frame_order_type": "loop", "frame_order": "", "frame_interpolate": False,
            "visible": True, "internal": True, "saved": False, "uuid": uid(), "source": data_uri(pixels)}

def keyframe(channel, time, x, y, z, interpolation="linear"):
    fmt = lambda v: ('%.4f' % v).rstrip('0').rstrip('.') if isinstance(v, float) else str(v)
    return {"channel": channel, "data_points": [{"x": fmt(x), "y": fmt(y), "z": fmt(z)}], "uuid": uid(),
            "time": round(time, 4), "color": -1, "interpolation": interpolation}

def main(vanilla, out, frames_dir=None):
    hotbar = read_png(os.path.join(vanilla, 'hotbar.png'))
    selection = read_png(os.path.join(vanilla, 'hotbar_selection.png'))
    sword = read_png(os.path.join(vanilla, 'netherite_sword.png'))
    assert len(selection) == SEL_H and len(selection[0]) == SEL_W

    # Static atlas: slot background crop | sword | padlock.
    atlas = blank(CANVAS + 16 + 7, CANVAS)
    for r in range(CANVAS):
        for c in range(CANVAS):
            hx, hy = c - 1 + 20 * HOTBAR_SLOT, r - 2
            if 0 <= hy < len(hotbar) and 0 <= hx < len(hotbar[0]):
                atlas[r][c] = hotbar[hy][hx]
    for r in range(16):
        for c in range(16):
            atlas[r][CANVAS + c] = sword[r][c]
    for r, line in enumerate(PADLOCK):
        for c, ch in enumerate(line):
            if ch in PADLOCK_COLORS:
                atlas[r][CANVAS + 16 + c] = argb(PADLOCK_COLORS[ch])

    # Flipbooks: frame k shows the state at k / FPS seconds.
    flipbooks = {}
    for locking in (True, False):
        frames = [fx_frame(selection, k * 1000 / FPS, locking) for k in range(FRAMES)]
        held = frames + [frames[-1]] * (HOLD_SECONDS * FPS - len(frames))
        flipbooks[locking] = [row for frame in held for row in frame]
        if frames_dir:
            os.makedirs(frames_dir, exist_ok=True)
            for k, frame in enumerate(frames):
                open(os.path.join(frames_dir, '%s_%02d.png' % ('lock' if locking else 'unlock', k)), 'wb').write(png_bytes(frame))

    textures = [texture('slot_atlas.png', atlas, len(atlas[0]), CANVAS, 0),
                texture('fx_lock.png', flipbooks[True], CANVAS, CANVAS, 1, True, FPS),
                texture('fx_unlock.png', flipbooks[False], CANVAS, CANVAS, 2, True, FPS)]

    px = 1 / 16  # small z gaps keep layer order stable
    bg = plane('slot_background', 0, 0, CANVAS, CANVAS, 0, 0, [0, 0, CANVAS, CANVAS])
    fx_lock = plane('frame_fx_lock', 0, 0, CANVAS, CANVAS, 1 * px, 1, [0, 0, CANVAS, CANVAS])
    fx_unlock = plane('frame_fx_unlock', 0, 0, CANVAS, CANVAS, 2 * px, 2, [0, 0, CANVAS, CANVAS])
    sword_el = plane('netherite_sword', *ITEM, 16, 16, 3 * px, 0, [CANVAS, 0, CANVAS + 16, 16])
    pu = CANVAS + 16
    shackle_el = plane('padlock_shackle', BADGE[0], BADGE[1], 7, 3, 4 * px, 0, [pu, 0, pu + 7, 3])
    body_el = plane('padlock_body', BADGE[0], BADGE[1] + 3, 7, 5, 4 * px, 0, [pu, 3, pu + 7, 8])

    badge_center = to_bb(BADGE[0] + 3.5, BADGE[1] + 4) + [4 * px]
    shackle_group = group('shackle', badge_center, [shackle_el['uuid']])
    padlock_group = group('padlock', badge_center, [body_el['uuid'], shackle_group])
    fx_lock_group = group('frame_fx_lock', [0, 0, 0], [fx_lock['uuid']])
    fx_unlock_group = group('frame_fx_unlock', [0, 0, 0], [fx_unlock['uuid']])
    root = group('locked_slot', [0, 0, 0], [bg['uuid'], fx_lock_group, fx_unlock_group, sword_el['uuid'], padlock_group])

    def animation(name, locking):
        scale_keys, lift_keys = [], []
        times = sorted({k / FPS for k in range(FRAMES)} | {BADGE_START / 1000, DURATION / 1000,
                       (DURATION - BADGE_START) / 1000})
        for t in times:
            s, lift = badge_state(t * 1000, locking)
            scale_keys.append(keyframe('scale', t, s, s, 1.0))
            lift_keys.append(keyframe('position', t, 0, lift, 0))
        hidden = fx_unlock_group if locking else fx_lock_group
        return {"uuid": uid(), "name": name, "loop": "hold", "override": False, "length": LENGTH,
                "snapping": FPS, "selected": False, "anim_time_update": "", "blend_weight": "",
                "start_delay": "", "loop_delay": "",
                "animators": {
                    padlock_group['uuid']: {"name": "padlock", "type": "bone", "keyframes": scale_keys},
                    shackle_group['uuid']: {"name": "shackle", "type": "bone", "keyframes": lift_keys},
                    hidden['uuid']: {"name": hidden['name'], "type": "bone",
                                     "keyframes": [keyframe('scale', 0, 0.0, 0.0, 0.0, 'step')]}}}

    model = {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
             "name": "hotbar_locker_slot", "model_identifier": "", "visible_box": [1, 1, 0],
             "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
             "unhandled_root_fields": {}, "resolution": {"width": len(atlas[0]), "height": CANVAS},
             "elements": [bg, fx_lock, fx_unlock, sword_el, shackle_el, body_el],
             "outliner": [root], "textures": textures,
             "animations": [animation('lock', True), animation('unlock', False)]}
    with open(out, 'w', encoding='utf-8') as f:
        json.dump(model, f, indent='\t')
    print('wrote', out, 'frames', FRAMES)

if __name__ == '__main__':
    main(*sys.argv[1:])
