import { Resvg } from '@resvg/resvg-js';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, '..');
const resRoot = path.join(root, 'app', 'src', 'main', 'res');
const logoPaths = fs.readFileSync(
  path.join(root, 'app', 'src', 'main', 'assets', 'logo_mark.svg'),
  'utf8',
).replace(/<svg[^>]*>/, '').replace('</svg>', '');

function squareSvg(size, { background = null, monochrome = false } = {}) {
  const pad = size * 0.20;
  const logoW = size - pad * 2;
  const logoH = logoW * (415 / 707);
  const x = pad;
  const y = (size - logoH) / 2;
  const fill = monochrome ? '#FFFFFF' : null;
  const paths = fill
    ? logoPaths.replace(/fill="#[^"]+"/g, `fill="${fill}"`)
    : logoPaths;

  const bgRect = background
    ? `<rect width="${size}" height="${size}" fill="${background}"/>`
    : '';

  return `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
  ${bgRect}
  <svg x="${x}" y="${y}" width="${logoW}" height="${logoH}" viewBox="0 0 707 415">
    ${paths}
  </svg>
</svg>`;
}

function renderPng(svg, size) {
  const resvg = new Resvg(svg, {
    fitTo: { mode: 'width', value: size },
    background: 'transparent',
  });
  return resvg.render().asPng();
}

const densities = {
  mdpi: 1,
  hdpi: 1.5,
  xhdpi: 2,
  xxhdpi: 3,
  xxxhdpi: 4,
};

function write(filePath, buffer) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, buffer);
  console.log('wrote', path.relative(root, filePath));
}

for (const [density, scale] of Object.entries(densities)) {
  const launcher = Math.round(48 * scale);
  const foreground = Math.round(108 * scale);
  const dir = path.join(resRoot, `mipmap-${density}`);

  write(
    path.join(dir, 'ic_launcher.png'),
    renderPng(squareSvg(launcher), launcher),
  );
  write(
    path.join(dir, 'ic_launcher_round.png'),
    renderPng(squareSvg(launcher), launcher),
  );
  write(
    path.join(dir, 'ic_launcher_foreground.png'),
    renderPng(squareSvg(foreground), foreground),
  );
  write(
    path.join(dir, 'ic_launcher_monochrome.png'),
    renderPng(squareSvg(foreground, { monochrome: true }), foreground),
  );
  write(
    path.join(dir, 'ic_launcher_background.png'),
    renderPng(`<svg width="${foreground}" height="${foreground}" xmlns="http://www.w3.org/2000/svg"/>`, foreground),
  );
  write(
    path.join(dir, 'ic_launcher_round_foreground.png'),
    renderPng(squareSvg(foreground), foreground),
  );
}

// Notification / in-app logo
const notifDir = path.join(resRoot, 'drawable-nodpi');
write(path.join(notifDir, 'ic_logo.png'), renderPng(squareSvg(128), 128));
write(path.join(notifDir, 'ic_logo_notif.png'), renderPng(squareSvg(96), 96));

console.log('Done.');
