// 시드 색에서 Material 3 라이트/다크 ColorScheme을 만들어 Color.kt로 쓴다.
// 근거: docs/design/06-design-direction.md 2.2. 런타임 의존 없이 정적 값으로 둔다.
//
// 실행(저장소 루트, node_modules를 만들지 않는다):
//   npx -y -p @material/material-color-utilities@0.4.0 node tools/gen_theme.mjs
//
// 0.4.0 패키지는 일부 ESM import에 확장자(.js)가 빠져 있어 Node가 바로 읽지 못한다.
// 아래 resolve 훅이 빠진 .js만 붙인다.
import { registerHooks } from 'node:module';
import { existsSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const VERSION = '0.4.0';
const SEED = '#0A81AB';
// 마감 임박 확장 색(06 2.3·브리핑 3a A4). Blend.harmonize로 시드 쪽으로 조화한다(blend: true).
const CLOSING_SOON = '#E8710A';
const OUT = 'shared/src/commonMain/kotlin/com/cherryzp/animalshelter/ui/theme/Color.kt';

registerHooks({
  resolve(specifier, context, next) {
    try {
      return next(specifier, context);
    } catch (e) {
      if (e.code === 'ERR_MODULE_NOT_FOUND' && specifier.startsWith('.') && !specifier.endsWith('.js')) {
        return next(`${specifier}.js`, context);
      }
      throw e;
    }
  },
});

// npx -p 가 PATH에 넣은 임시 설치 경로에서 패키지를 찾는다.
const pkgDir = (process.env.PATH ?? '')
  .split(':')
  .filter((p) => p.endsWith('node_modules/.bin'))
  .map((p) => join(dirname(p), '@material', 'material-color-utilities'))
  .find((p) => existsSync(join(p, 'index.js')));
if (!pkgDir) {
  console.error(`@material/material-color-utilities@${VERSION}를 찾지 못했다. npx -p로 실행한다.`);
  process.exit(1);
}
const m = await import(pathToFileURL(join(pkgDir, 'index.js')).href);

// Compose Material 3 1.9.0(Jetpack 1.4.0) lightColorScheme/darkColorScheme 인자 순서
const ROLES = [
  'primary', 'onPrimary', 'primaryContainer', 'onPrimaryContainer', 'inversePrimary',
  'secondary', 'onSecondary', 'secondaryContainer', 'onSecondaryContainer',
  'tertiary', 'onTertiary', 'tertiaryContainer', 'onTertiaryContainer',
  'background', 'onBackground', 'surface', 'onSurface', 'surfaceVariant', 'onSurfaceVariant',
  'surfaceTint', 'inverseSurface', 'inverseOnSurface',
  'error', 'onError', 'errorContainer', 'onErrorContainer',
  'outline', 'outlineVariant', 'scrim',
  'surfaceBright', 'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest',
  'surfaceContainerLow', 'surfaceContainerLowest', 'surfaceDim',
  'primaryFixed', 'primaryFixedDim', 'onPrimaryFixed', 'onPrimaryFixedVariant',
  'secondaryFixed', 'secondaryFixedDim', 'onSecondaryFixed', 'onSecondaryFixedVariant',
  'tertiaryFixed', 'tertiaryFixedDim', 'onTertiaryFixed', 'onTertiaryFixedVariant',
];

const hex = (argb) => (argb >>> 0).toString(16).toUpperCase().padStart(8, '0');
const scheme = (isDark) => {
  // 스펙 2021 = Material Theme Builder·Compose Material 3 1.4 기준 톤
  const s = new m.SchemeTonalSpot(m.Hct.fromInt(m.argbFromHex(SEED)), isDark, 0.0, '2021');
  return ROLES.map((role) => {
    const dc = m.MaterialDynamicColors[role];
    if (!dc) throw new Error(`역할 없음: ${role}`);
    return `    ${role} = Color(0x${hex(dc.getArgb(s))})`;
  }).join(',\n'); // 호출부 끝 쉼표는 두지 않는다(.editorconfig)
};

// customColor(0.4.0, utils/theme_utils): 라이트 colorContainer/onColorContainer = 톤 90/10,
// 다크 = 톤 30/90.
if (typeof m.customColor !== 'function') throw new Error('customColor를 내보내지 않는다');
const closingSoon = m.customColor(m.argbFromHex(SEED), {
  name: 'closingSoon',
  value: m.argbFromHex(CLOSING_SOON),
  blend: true,
});
const extended = (group) => `ExtendedColors(
    closingSoonContainer = Color(0x${hex(group.colorContainer)}),
    onClosingSoonContainer = Color(0x${hex(group.onColorContainer)})
)`;

// WCAG 대비(글자/배경). 보고용으로 콘솔에만 찍는다.
const luminance = (argb) => {
  const ch = [16, 8, 0].map((sh) => {
    const c = ((argb >> sh) & 0xff) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * ch[0] + 0.7152 * ch[1] + 0.0722 * ch[2];
};
const contrast = (fg, bg) => {
  const [a, b] = [luminance(fg), luminance(bg)].sort((x, y) => y - x);
  return ((a + 0.05) / (b + 0.05)).toFixed(2);
};

const kt = `// 생성 파일: tools/gen_theme.mjs (직접 고치지 않는다)
// 시드 ${SEED}, 스킴 TonalSpot(스펙 2021, 대비 0), @material/material-color-utilities ${VERSION}
// 명령: npx -y -p @material/material-color-utilities@${VERSION} node tools/gen_theme.mjs
// 확장 색 closingSoon: 원색 ${CLOSING_SOON}, customColor(blend = true → Blend.harmonize(원색, 시드))
//   라이트 톤 90/10: ${hex(closingSoon.light.colorContainer)}/${hex(closingSoon.light.onColorContainer)}, 대비 ${contrast(closingSoon.light.onColorContainer, closingSoon.light.colorContainer)}:1
//   다크 톤 30/90: ${hex(closingSoon.dark.colorContainer)}/${hex(closingSoon.dark.onColorContainer)}, 대비 ${contrast(closingSoon.dark.onColorContainer, closingSoon.dark.colorContainer)}:1
package com.cherryzp.animalshelter.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** 브랜드 원색. 스플래시·로고·웹 로딩 화면에서만 그대로 쓴다(UI는 색 역할만). */
val BrandSeed = Color(0xFF${SEED.slice(1).toUpperCase()})

val LightColorScheme: ColorScheme = lightColorScheme(
${scheme(false)}
)

val DarkColorScheme: ColorScheme = darkColorScheme(
${scheme(true)}
)

/** 라이트 확장 색(마감 임박 배지). */
val LightExtendedColors: ExtendedColors = ${extended(closingSoon.light)}

/** 다크 확장 색(마감 임박 배지). */
val DarkExtendedColors: ExtendedColors = ${extended(closingSoon.dark)}
`;

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
writeFileSync(join(root, OUT), kt);
console.log(`썼다: ${OUT}`);
for (const [name, g] of [['light', closingSoon.light], ['dark', closingSoon.dark]]) {
  console.log(`closingSoon ${name}: container ${hex(g.colorContainer)} on ${hex(g.onColorContainer)} 대비 ${contrast(g.onColorContainer, g.colorContainer)}:1`);
}
