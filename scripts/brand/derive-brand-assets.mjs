/**
 * ZS-BRAND-003.A 品牌素材派生（docs/06 第 6 节）。
 *
 * 仅对用户原图（docs/assets/brand/zszj-logo-user-reference.png）做透明度利用、
 * 等比缩放与徽标区裁切，不重绘、不变形：
 *   1. 完整横版 Logo（原样字节复制，用于展示位）；
 *   2. 左侧云形徽标方形裁切（自动检测徽标与文字之间的空白列间隙）；
 *   3. 方形图标等比缩放（盒式滤波）并合成 favicon.ico（PNG-in-ICO：16/32/48/256）。
 * 用法：node scripts/brand/derive-brand-assets.mjs
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateSync, inflateSync as infl } from 'node:zlib';

const root = fileURLToPath(new URL('../../', import.meta.url));
const SRC = join(root, 'docs', 'assets', 'brand', 'zszj-logo-user-reference.png');

function decodePng(buf) {
  if (buf.readUInt32BE(0) !== 0x89504e47) throw new Error('not a png');
  let off = 8, w = 0, h = 0, bitDepth = 0, colorType = 0;
  const idat = [];
  while (off < buf.length) {
    const len = buf.readUInt32BE(off);
    const type = buf.toString('ascii', off + 4, off + 8);
    const data = buf.subarray(off + 8, off + 8 + len);
    if (type === 'IHDR') { w = data.readUInt32BE(0); h = data.readUInt32BE(4); bitDepth = data[8]; colorType = data[9]; }
    else if (type === 'IDAT') idat.push(data);
    else if (type === 'IEND') break;
    off += 12 + len;
  }
  if (bitDepth !== 8 || colorType !== 6) throw new Error(`unsupported png: depth=${bitDepth} color=${colorType}`);
  const inflated = infl(Buffer.concat(idat));
  const stride = w * 4;
  const px = Buffer.alloc(h * stride);
  let p = 0;
  for (let y = 0; y < h; y++) {
    const filter = inflated[p++];
    const row = inflated.subarray(p, p + stride); p += stride;
    for (let x = 0; x < stride; x++) {
      const a = x >= 4 ? px[y * stride + x - 4] : 0;
      const b = y > 0 ? px[(y - 1) * stride + x] : 0;
      const c = x >= 4 && y > 0 ? px[(y - 1) * stride + x - 4] : 0;
      let v = row[x];
      if (filter === 1) v += a;
      else if (filter === 2) v += b;
      else if (filter === 3) v += Math.floor((a + b) / 2);
      else if (filter === 4) {
        const pa = Math.abs(b - c), pb = Math.abs(a - c), pc = Math.abs(a + b - 2 * c);
        v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
      }
      px[y * stride + x] = v & 255;
    }
  }
  return { w, h, px };
}

function encodePng(w, h, px) {
  const crcTable = (() => {
    const t = new Int32Array(256);
    for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c; }
    return t;
  })();
  const crc32 = (buf) => {
    let c = 0xffffffff;
    for (const byte of buf) c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8);
    return (c ^ 0xffffffff) >>> 0;
  };
  const chunk = (type, data) => {
    const out = Buffer.alloc(12 + data.length);
    out.writeUInt32BE(data.length, 0);
    out.write(type, 4, 'ascii');
    data.copy(out, 8);
    out.writeUInt32BE(crc32(out.subarray(4, 8 + data.length)), 8 + data.length);
    return out;
  };
  const stride = w * 4;
  const raw = Buffer.alloc((stride + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (stride + 1)] = 0;
    px.subarray(y * stride, (y + 1) * stride).copy(raw, y * (stride + 1) + 1);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6; // 8-bit RGBA
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

/** 盒式滤波等比缩放（RGBA），目标区域外为透明。 */
function resize(src, dw, dh) {
  const out = Buffer.alloc(dw * dh * 4);
  for (let y = 0; y < dh; y++) {
    const y0 = Math.floor((y * src.h) / dh), y1 = Math.max(y0 + 1, Math.floor(((y + 1) * src.h) / dh));
    for (let x = 0; x < dw; x++) {
      const x0 = Math.floor((x * src.w) / dw), x1 = Math.max(x0 + 1, Math.floor(((x + 1) * src.w) / dw));
      let r = 0, g = 0, b = 0, a = 0, n = 0;
      for (let sy = y0; sy < y1; sy++) for (let sx = x0; sx < x1; sx++) {
        const i = (sy * src.w + sx) * 4;
        const sa = src.px[i + 3];
        r += src.px[i] * sa; g += src.px[i + 1] * sa; b += src.px[i + 2] * sa; a += sa; n++;
      }
      const o = (y * dw + x) * 4;
      if (a > 0) { out[o] = Math.round(r / a); out[o + 1] = Math.round(g / a); out[o + 2] = Math.round(b / a); out[o + 3] = Math.round(a / n); }
    }
  }
  return { w: dw, h: dh, px: out };
}

function crop(src, x0, y0, x1, y1) {
  const w = x1 - x0, h = y1 - y0, out = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) src.px.subarray(((y + y0) * src.w + x0) * 4, ((y + y0) * src.w + x1) * 4).copy(out, y * w * 4);
  return { w, h, px: out };
}

function padToSquare(img, pad = 0.06) {
  const size = Math.ceil(Math.max(img.w, img.h) * (1 + pad * 2));
  const out = Buffer.alloc(size * size * 4);
  const ox = Math.floor((size - img.w) / 2), oy = Math.floor((size - img.h) / 2);
  for (let y = 0; y < img.h; y++) img.px.subarray(y * img.w * 4, (y + 1) * img.w * 4).copy(out, ((y + oy) * size + ox) * 4);
  return { w: size, h: size, px: out };
}

// ---- 主流程 ----
const src = decodePng(readFileSync(SRC));

// 列占用统计，找出徽标与文字之间的最大空白间隙
const colAlpha = new Array(src.w).fill(0);
for (let x = 0; x < src.w; x++) for (let y = 0; y < src.h; y++) colAlpha[x] += src.px[(y * src.w + x) * 4 + 3];
let gapStart = -1, gapLen = 0, best = { start: 0, len: 0 };
for (let x = 0; x < src.w; x++) {
  if (colAlpha[x] === 0) { if (gapStart < 0) gapStart = x; gapLen++; }
  else { if (gapStart >= 0 && gapLen > best.len && gapStart > 0) best = { start: gapStart, len: gapLen }; gapStart = -1; gapLen = 0; }
}
if (best.len === 0) throw new Error('emblem/text gap not found');
const emblemX1 = best.start + Math.floor(best.len / 2);

// 徽标内容包围盒
let bx0 = src.w, bx1 = 0, by0 = src.h, by1 = 0;
for (let x = 0; x < emblemX1; x++) for (let y = 0; y < src.h; y++) {
  if (src.px[(y * src.w + x) * 4 + 3] > 0) { if (x < bx0) bx0 = x; if (x >= bx1) bx1 = x + 1; if (y < by0) by0 = y; if (y >= by1) by1 = y + 1; }
}
const emblem = padToSquare(crop(src, bx0, by0, bx1, by1));

const outputs = [
  // Web 端 Logo 位（导航/登录为方形显示位）使用徽标方形图
  ['apps/zhongshu-admin-web/src/assets/imgs/logo.png', encodePng(emblem.w, emblem.h, emblem.px)],
  ['apps/zhongshu-miniapp/src/static/logo.png', encodePng(emblem.w, emblem.h, emblem.px)],
  // 衍生徽标留档（512），便于后续 App 图标等场景复用
  ['docs/assets/brand/zszj-emblem-512.png', encodePng(512, 512, resize(emblem, 512, 512).px)],
];

// favicon.ico：16/32/48/256 PNG-in-ICO
const sizes = [16, 32, 48, 256];
const pngs = sizes.map((s) => encodePng(s, s, resize(emblem, s, s).px));
const header = Buffer.alloc(6);
header.writeUInt16LE(0, 0); header.writeUInt16LE(1, 2); header.writeUInt16LE(sizes.length, 4);
let offset = 6 + 16 * sizes.length;
const dirs = [], blobs = [];
for (let i = 0; i < sizes.length; i++) {
  const d = Buffer.alloc(16);
  d[0] = sizes[i] === 256 ? 0 : sizes[i]; d[1] = sizes[i] === 256 ? 0 : sizes[i];
  d[2] = 0; d[3] = 0; d.writeUInt16LE(1, 4); d.writeUInt16LE(32, 6);
  d.writeUInt32LE(pngs[i].length, 8); d.writeUInt32LE(offset, 12);
  offset += pngs[i].length;
  dirs.push(d); blobs.push(pngs[i]);
}
outputs.push(['apps/zhongshu-admin-web/public/favicon.ico', Buffer.concat([header, ...dirs, ...blobs])]);
outputs.push(['apps/zhongshu-miniapp/favicon.ico', Buffer.concat([header, ...dirs, ...blobs])]);

// 移动端 App 启动图标（manifest 引用的 static/app/icons 尺寸），由徽标等比缩放覆盖
const appIconSizes = [1024, 192, 180, 167, 152, 144, 120, 96, 87, 80, 76, 72, 60, 58, 40, 29, 20];
for (const s of appIconSizes) {
  outputs.push([`apps/zhongshu-miniapp/src/static/app/icons/${s}x${s}.png`, encodePng(s, s, resize(emblem, s, s).px)]);
}

for (const [rel, data] of outputs) {
  const target = join(root, rel);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, data);
  console.log(`written: ${rel} (${data.length} bytes)`);
}
console.log(`emblem bbox: x[${bx0},${bx1}) y[${by0},${by1}), gap at x=${best.start} len=${best.len}`);
