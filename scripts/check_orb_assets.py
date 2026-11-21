#!/usr/bin/env python3
"""No-dependency CI pixel check for neutral, transparent RGBA orb assets."""
from pathlib import Path
import struct
import zlib

PNG = b"\x89PNG\r\n\x1a\n"
ASSETS = ("orb_core", "orb_petals", "orb_ring")
ROOT = Path("app/src/main/res/drawable-nodpi")


def pixels(path: Path):
    content = path.read_bytes()
    assert content.startswith(PNG), f"{path}: bad PNG signature"
    pos = len(PNG)
    compressed = bytearray()
    shape = None
    while pos + 12 <= len(content):
        length = struct.unpack(">I", content[pos:pos + 4])[0]
        tag = content[pos + 4:pos + 8]
        data = content[pos + 8:pos + 8 + length]
        crc = struct.unpack(">I", content[pos + 8 + length:pos + 12 + length])[0]
        assert len(data) == length, f"{path}: truncated chunk"
        assert (zlib.crc32(tag + data) & 0xffffffff) == crc, f"{path}: bad {tag!r} CRC"
        pos += length + 12
        if tag == b"IHDR":
            shape = struct.unpack(">IIBBBBB", data)
        elif tag == b"IDAT":
            compressed.extend(data)
        elif tag == b"IEND":
            break
    assert shape is not None, f"{path}: missing IHDR"
    width, height, depth, color, compression, filtering, interlace = shape
    assert width >= 64 and height >= 64, f"{path}: too small"
    assert (depth, color, compression, filtering, interlace) == (8, 6, 0, 0, 0), (
        f"{path}: expected noninterlaced RGBA PNG"
    )
    scanlines = zlib.decompress(compressed)
    stride = width * 4
    expected = (stride + 1) * height
    assert len(scanlines) == expected, f"{path}: unexpected scanline length"

    prior = bytearray(stride)
    index = 0
    for _ in range(height):
        filter_type = scanlines[index]
        index += 1
        current = bytearray(scanlines[index:index + stride])
        index += stride
        assert filter_type in range(5), f"{path}: unsupported PNG filter {filter_type}"
        for col in range(stride):
            left = current[col - 4] if col >= 4 else 0
            above = prior[col]
            upper_left = prior[col - 4] if col >= 4 else 0
            if filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = above
            elif filter_type == 3:
                predictor = (left + above) // 2
            elif filter_type == 4:
                p = left + above - upper_left
                distances = (abs(p - left), abs(p - above), abs(p - upper_left))
                predictor = (left, above, upper_left)[distances.index(min(distances))]
            else:
                predictor = 0
            current[col] = (current[col] + predictor) & 255
        for col in range(0, stride, 4):
            yield current[col:col + 4]
        prior = current


def main():
    for layer in ASSETS:
        path = ROOT / (layer + ".png")
        assert path.is_file(), f"Missing {path}"
        clear = visible = shaded = 0
        for red, green, blue, alpha in pixels(path):
            if alpha == 0:
                clear += 1
                continue
            visible += 1
            assert abs(red - green) <= 1 and abs(green - blue) <= 1, (
                f"{path}: color embedded in grayscale source ({red},{green},{blue})"
            )
            if 15 < red < 240:
                shaded += 1
        assert clear > 40 and visible > 40 and shaded > 30, (
            f"{path}: alpha/background/shading check failed "
            f"(clear={clear}, visible={visible}, shades={shaded})"
        )
        print(f"PASS {layer}: grayscale, transparent, shaded "
              f"({clear} transparent, {visible} visible pixels)")


if __name__ == "__main__":
    main()
