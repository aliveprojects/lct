import type { JSX } from 'preact';

// Все иконки нарисованы для этого проекта (лицензия — как у остального кода репозитория).
// Два набора: контурные (24×24, берут цвет текста) и цветные иллюстрации (48×48).

type Draw = () => JSX.Element;

const S = { fill: 'none', stroke: 'currentColor', 'stroke-width': 2.4, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' } as const;

const LINE: Record<string, Draw> = {
  back: () => <path d="M15 5l-7 7 7 7" {...S} />,
  help: () => (
    <g {...S}>
      <circle cx="12" cy="12" r="9.5" />
      <path d="M9.4 9.4a2.7 2.7 0 1 1 3.9 2.4c-.9.5-1.3 1-1.3 2" />
      <path d="M12 17.3v.1" />
    </g>
  ),
  settings: () => (
    <g {...S}>
      <path d="M4 7h9M17 7h3M4 17h3M11 17h9M4 12h16" />
      <circle cx="15" cy="7" r="2" />
      <circle cx="9" cy="17" r="2" />
    </g>
  ),
  check: () => <path d="M5 12.5l4.5 4.5L19 7.5" {...S} />,
  cross: () => <path d="M6 6l12 12M18 6L6 18" {...S} />,
  plus: () => <path d="M12 5v14M5 12h14" {...S} />,
  minus: () => <path d="M5 12h14" {...S} />,
  arrow: () => <path d="M5 12h14M13 6l6 6-6 6" {...S} />,
  heart: () => <path d="M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.4A4 4 0 0 1 19 10c0 5.6-7 10-7 10z" {...S} />,
  sound: () => (
    <g {...S}>
      <path d="M4 10v4h4l5 4V6L8 10z" />
      <path d="M16.5 9a4 4 0 0 1 0 6M18.8 6.5a7.5 7.5 0 0 1 0 11" />
    </g>
  ),
  mute: () => (
    <g {...S}>
      <path d="M4 10v4h4l5 4V6L8 10z" />
      <path d="M17 9.5l4 5M21 9.5l-4 5" />
    </g>
  ),
  history: () => (
    <g {...S}>
      <path d="M4 12a8 8 0 1 0 2.5-5.8L4 8.5" />
      <path d="M4 4v4.5h4.5M12 8v4.5l3 2" />
    </g>
  ),
  book: () => (
    <g {...S}>
      <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5z" />
      <path d="M4 20.5A2.5 2.5 0 0 0 6.5 23H20v-5" />
    </g>
  ),
  trash: () => (
    <g {...S}>
      <path d="M4 7h16M9 7V4h6v3M6.5 7l1 13h9l1-13M10 11v5M14 11v5" />
    </g>
  ),
  refresh: () => (
    <g {...S}>
      <path d="M20 12a8 8 0 1 1-2.5-5.8L20 8.5" />
      <path d="M20 4v4.5h-4.5" />
    </g>
  ),
  lock: () => (
    <g {...S}>
      <rect x="5" y="11" width="14" height="10" rx="2.5" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </g>
  ),
  sparkle: () => <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z" {...S} />,
  info: () => (
    <g {...S}>
      <circle cx="12" cy="12" r="9.5" />
      <path d="M12 11v6M12 7.3v.1" />
    </g>
  ),
  bulb: () => (
    <g {...S}>
      <path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z" />
    </g>
  ),
};

const ART: Record<string, Draw> = {
  coin: () => (
    <g>
      <circle cx="24" cy="24" r="19" fill="#F5B301" stroke="#D18F00" stroke-width="3" />
      <circle cx="24" cy="24" r="12.5" fill="none" stroke="#FFE07A" stroke-width="3" />
      <path d="M20 28l4-9 4 9M21.3 25.5h5.4" stroke="#B87700" stroke-width="2.6" fill="none" stroke-linecap="round" stroke-linejoin="round" />
    </g>
  ),
  jar: () => (
    <g>
      <ellipse cx="24" cy="27" rx="17" ry="13.5" fill="#F7A3B8" />
      <path d="M12 17l-3-8 9 4z" fill="#F58BA6" />
      <ellipse cx="41" cy="28" rx="5.5" ry="4.6" fill="#F58BA6" />
      <circle cx="39.5" cy="27.5" r="1" fill="#9E3652" />
      <circle cx="42.6" cy="27.5" r="1" fill="#9E3652" />
      <circle cx="32" cy="22" r="2" fill="#2B2540" />
      <rect x="17" y="12.5" width="12" height="3.4" rx="1.7" fill="#B84A68" />
      <rect x="14" y="37" width="6" height="7" rx="2" fill="#F58BA6" />
      <rect x="30" y="37" width="6" height="7" rx="2" fill="#F58BA6" />
      <path d="M7.5 25c-3 0-4 3-2 4.5" stroke="#F58BA6" stroke-width="2.4" fill="none" stroke-linecap="round" />
    </g>
  ),
  bowl: () => (
    <g>
      <path d="M5 23h38c0 10-8 17-19 17S5 33 5 23z" fill="#F08A4B" />
      <path d="M9 23c2-8 8-11 15-11s13 3 15 11z" fill="#8E5B2C" />
      <circle cx="18" cy="17.5" r="2.7" fill="#B98046" />
      <circle cx="27" cy="15.5" r="2.7" fill="#B98046" />
      <circle cx="32" cy="20" r="2.7" fill="#B98046" />
      <rect x="14" y="39" width="20" height="4" rx="2" fill="#C96E35" />
    </g>
  ),
  fish: () => (
    <g>
      <path d="M5 24c6-10 19-13 29-4l9-6v20l-9-6C24 37 11 34 5 24z" fill="#4FA3E0" />
      <path d="M22 17c3 4 3 10 0 14" stroke="#2F7DBA" stroke-width="2.4" fill="none" stroke-linecap="round" />
      <circle cx="13" cy="22" r="2.6" fill="#fff" />
      <circle cx="13" cy="22" r="1.2" fill="#2B2540" />
    </g>
  ),
  bath: () => (
    <g>
      <circle cx="16" cy="13" r="5" fill="#E8F6FF" stroke="#7CC4F5" stroke-width="2.4" />
      <circle cx="28" cy="9" r="3.5" fill="#E8F6FF" stroke="#7CC4F5" stroke-width="2.4" />
      <circle cx="33" cy="16" r="4.5" fill="#E8F6FF" stroke="#7CC4F5" stroke-width="2.4" />
      <path d="M5 23h38v4c0 8-6 14-14 14H19C11 41 5 35 5 27z" fill="#7CC4F5" />
      <rect x="3" y="21" width="42" height="5" rx="2.5" fill="#B9E1FA" />
      <rect x="10" y="41" width="5" height="4" rx="1.5" fill="#6B7A99" />
      <rect x="33" y="41" width="5" height="4" rx="1.5" fill="#6B7A99" />
    </g>
  ),
  brush: () => (
    <g>
      <rect x="4" y="16" width="28" height="16" rx="8" fill="#E48ABB" />
      <rect x="30" y="21" width="15" height="7" rx="3.5" fill="#B0653F" />
      <g fill="#fff">
        <circle cx="12" cy="21" r="1.7" />
        <circle cx="18" cy="21" r="1.7" />
        <circle cx="24" cy="21" r="1.7" />
        <circle cx="15" cy="27" r="1.7" />
        <circle cx="21" cy="27" r="1.7" />
      </g>
    </g>
  ),
  vet: () => (
    <g>
      <path d="M17 14v-3a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v3" fill="none" stroke="#2BA36B" stroke-width="3" />
      <rect x="5" y="14" width="38" height="28" rx="6" fill="#F2F7FF" stroke="#2BA36B" stroke-width="3" />
      <path d="M24 21v14M17 28h14" stroke="#2BA36B" stroke-width="5" stroke-linecap="round" />
    </g>
  ),
  vitamins: () => (
    <g>
      <rect x="14" y="6" width="20" height="9" rx="3" fill="#F5B301" />
      <rect x="11" y="14" width="26" height="30" rx="6" fill="#FFE39A" />
      <rect x="15" y="23" width="18" height="13" rx="3" fill="#fff" />
      <circle cx="21" cy="29.5" r="3.2" fill="#F08A4B" />
      <circle cx="28" cy="29.5" r="3.2" fill="#8ED08B" />
    </g>
  ),
  ball: () => (
    <g>
      <circle cx="24" cy="24" r="18" fill="#F2647A" />
      <path d="M24 6c-7 8-7 28 0 36M6.5 22c8 5 27 5 35 0" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" />
      <ellipse cx="17" cy="15" rx="4" ry="2.6" fill="#fff" opacity=".35" transform="rotate(-35 17 15)" />
    </g>
  ),
  mouse: () => (
    <g>
      <path d="M40 30c6 1 6 8 1 10" stroke="#A9B4C9" stroke-width="3" fill="none" stroke-linecap="round" />
      <ellipse cx="22" cy="28" rx="17" ry="11" fill="#A9B4C9" />
      <circle cx="13" cy="17" r="6.5" fill="#A9B4C9" />
      <circle cx="13" cy="17" r="3.6" fill="#F7B8C6" />
      <circle cx="28" cy="16" r="6.5" fill="#A9B4C9" />
      <circle cx="28" cy="16" r="3.6" fill="#F7B8C6" />
      <circle cx="33" cy="27" r="1.9" fill="#2B2540" />
      <circle cx="6.5" cy="29" r="2.2" fill="#F2647A" />
    </g>
  ),
  cake: () => (
    <g>
      <path d="M8 26h32l-4 16H12z" fill="#E7A35F" />
      <path d="M7 27c0-8 6-11 17-11s17 3 17 11z" fill="#F7B8D0" />
      <circle cx="24" cy="12" r="4.2" fill="#E4534B" />
      <path d="M24 8c0-3 2-4 4-4" stroke="#4CAF50" stroke-width="2.4" fill="none" stroke-linecap="round" />
      <path d="M13 27v3M20 27v4M28 27v3M35 27v4" stroke="#fff" stroke-width="2.4" stroke-linecap="round" />
    </g>
  ),
  flower: () => (
    <g>
      <path d="M24 26v18" stroke="#4CAF50" stroke-width="3" stroke-linecap="round" />
      <path d="M24 38c-6-1-9-4-10-8 5 0 9 3 10 8z" fill="#6CCB6F" />
      <g fill="#F98CA0">
        <circle cx="24" cy="10" r="7" />
        <circle cx="35" cy="18" r="7" />
        <circle cx="31" cy="30" r="7" transform="translate(-7 -3)" />
        <circle cx="13" cy="18" r="7" />
        <circle cx="17" cy="26" r="7" transform="translate(-1 -3)" />
      </g>
      <circle cx="24" cy="20" r="5.5" fill="#F5B301" />
    </g>
  ),
  scarf: () => (
    <g>
      <path d="M30 22l8 21h-10l-5-19z" fill="#E4534B" />
      <path d="M6 12c9 6 27 6 36 0v11c-9 6-27 6-36 0z" fill="#E4534B" />
      <path d="M14 15v11M22 17v11M30 17v11M37 15v10" stroke="#fff" stroke-width="2.6" opacity=".8" stroke-linecap="round" />
    </g>
  ),
  hat: () => (
    <g>
      <rect x="13" y="6" width="22" height="26" rx="3.5" fill="#3B3B6B" />
      <rect x="13" y="23" width="22" height="6" fill="#E4534B" />
      <ellipse cx="24" cy="34" rx="20" ry="6.5" fill="#4A4A80" />
    </g>
  ),
  castle: () => (
    <g>
      <rect x="4" y="16" width="12" height="26" fill="#B79CF0" />
      <rect x="32" y="16" width="12" height="26" fill="#B79CF0" />
      <rect x="14" y="22" width="20" height="20" fill="#CDB8F7" />
      <path d="M3 16h14v-4h-3v-3h-3v3H9v-3H6v3H3zM31 16h14v-4h-3v-3h-3v3h-2v-3h-3v3h-3z" fill="#B79CF0" />
      <path d="M20 42v-8a4 4 0 0 1 8 0v8z" fill="#7B5CC7" />
      <path d="M24 12V4l7 2.5-7 2.5" fill="#F2647A" stroke="#7B5CC7" stroke-width="2" stroke-linejoin="round" />
      <rect x="8" y="24" width="4" height="6" rx="2" fill="#7B5CC7" />
      <rect x="36" y="24" width="4" height="6" rx="2" fill="#7B5CC7" />
    </g>
  ),
  house: () => (
    <g>
      <rect x="9" y="22" width="30" height="20" rx="2" fill="#FFD9A0" />
      <path d="M4 25L24 6l20 19z" fill="#E4534B" />
      <rect x="20" y="30" width="9" height="12" rx="2" fill="#B0653F" />
      <rect x="12" y="27" width="6" height="6" rx="1" fill="#9BD3F5" />
      <rect x="31" y="27" width="5" height="6" rx="1" fill="#9BD3F5" />
      <rect x="32" y="9" width="5" height="9" fill="#B0653F" />
    </g>
  ),
  book: () => (
    <g>
      <path d="M5 9h16c2.5 0 4 1.3 4 4v29c0-2.7-1.5-4-4-4H5z" fill="#4C6FE0" />
      <path d="M43 9H27c-2.5 0-4 1.3-4 4v29c0-2.7 1.5-4 4-4h16z" fill="#7C98F0" />
      <path d="M9 16h10M9 21h10M29 16h10M29 21h10" stroke="#fff" stroke-width="2.2" stroke-linecap="round" opacity=".85" />
    </g>
  ),
  park: () => (
    <g>
      <path d="M24 21L13 45M24 21l11 24" stroke="#6B7A99" stroke-width="3" stroke-linecap="round" />
      <circle cx="24" cy="21" r="15" fill="none" stroke="#4CB8A0" stroke-width="3" />
      <path d="M24 6v30M9 21h30M13.5 10.5l21 21M34.5 10.5l-21 21" stroke="#4CB8A0" stroke-width="1.8" />
      <circle cx="24" cy="6" r="3.2" fill="#F2647A" />
      <circle cx="39" cy="21" r="3.2" fill="#F5B301" />
      <circle cx="24" cy="36" r="3.2" fill="#4FA3E0" />
      <circle cx="9" cy="21" r="3.2" fill="#B79CF0" />
      <circle cx="24" cy="21" r="3" fill="#fff" stroke="#4CB8A0" stroke-width="2" />
    </g>
  ),
  party: () => (
    <g>
      <path d="M17 29l7 15M31 27l-7 17M24 37v7" stroke="#6B7A99" stroke-width="1.8" fill="none" />
      <ellipse cx="16" cy="18" rx="9" ry="11" fill="#F2647A" />
      <ellipse cx="32" cy="16" rx="9" ry="11" fill="#F5B301" />
      <ellipse cx="24" cy="27" rx="9" ry="11" fill="#4FA3E0" />
      <ellipse cx="12.5" cy="14" rx="2" ry="3" fill="#fff" opacity=".45" />
    </g>
  ),
  gift: () => (
    <g>
      <rect x="6" y="21" width="36" height="21" rx="3" fill="#F2647A" />
      <rect x="4" y="15" width="40" height="8" rx="3" fill="#F98CA0" />
      <rect x="21" y="15" width="6" height="27" fill="#F5B301" />
      <path d="M24 15c-6 0-9-8-4-8 3 0 4 4 4 8zM24 15c6 0 9-8 4-8-3 0-4 4-4 8z" fill="#F5B301" />
    </g>
  ),
  star: () => (
    <path d="M24 5l5.6 12.2 13.4 1.4-10 9 2.9 13.2L24 34l-11.9 6.8 2.9-13.2-10-9 13.4-1.4z" fill="#F5B301" stroke="#D18F00" stroke-width="2" stroke-linejoin="round" />
  ),
  jacket: () => (
    <g>
      <path d="M17 7l7 5 7-5 11 9-5 9-6-3v21H17V22l-6 3-5-9z" fill="#4C6FE0" />
      <path d="M24 12v30" stroke="#fff" stroke-width="2.4" opacity=".8" />
      <circle cx="24" cy="18" r="1.6" fill="#fff" />
      <circle cx="24" cy="25" r="1.6" fill="#fff" />
    </g>
  ),
  ticket: () => (
    <g>
      <path d="M4 13h40v8a3.5 3.5 0 0 0 0 7v8H4v-8a3.5 3.5 0 0 0 0-7z" fill="#F5B301" />
      <path d="M31 14v20" stroke="#B87700" stroke-width="2.4" stroke-dasharray="3 3" />
      <path d="M16.5 19l2 4.3 4.6.5-3.4 3.1 1 4.6-4.2-2.4-4.2 2.4 1-4.6L9.900 23.800l4.600-.5z" fill="#fff" />
    </g>
  ),
  bow: () => (
    <g>
      <path d="M24 24L7 13v22zM24 24l17-11v22z" fill="#F2647A" />
      <circle cx="24" cy="24" r="5.5" fill="#D9425A" />
    </g>
  ),
  clip: () => (
    <g>
      <rect x="9" y="7" width="30" height="36" rx="5" fill="#fff" stroke="#4C6FE0" stroke-width="3" />
      <rect x="17" y="3" width="14" height="8" rx="3" fill="#4C6FE0" />
      <path d="M15 21h18M15 28h18M15 35h10" stroke="#9BB0F3" stroke-width="3" stroke-linecap="round" />
      <circle cx="35" cy="35" r="1" fill="none" />
    </g>
  ),
  bag: () => (
    <g>
      <path d="M17 17v-4a7 7 0 0 1 14 0v4" fill="none" stroke="#B0653F" stroke-width="3.2" stroke-linecap="round" />
      <path d="M8 16h32l3 26H5z" fill="#F08A4B" />
      <circle cx="18" cy="23" r="2" fill="#fff" />
      <circle cx="30" cy="23" r="2" fill="#fff" />
    </g>
  ),
  trophy: () => (
    <g>
      <path d="M13 8h22v11a11 11 0 0 1-22 0z" fill="#F5B301" />
      <path d="M13 12H6c0 7 3 11 8 12M35 12h7c0 7-3 11-8 12" fill="none" stroke="#D18F00" stroke-width="3" stroke-linecap="round" />
      <rect x="21" y="29" width="6" height="8" fill="#D18F00" />
      <rect x="14" y="37" width="20" height="6" rx="2" fill="#B87700" />
      <path d="M19 13v6" stroke="#FFE07A" stroke-width="3" stroke-linecap="round" />
    </g>
  ),
  key: () => (
    <g>
      <rect x="9" y="22" width="30" height="21" rx="5" fill="#7B5CC7" />
      <path d="M15 22v-6a9 9 0 0 1 18 0v6" fill="none" stroke="#5A3FA8" stroke-width="4" stroke-linecap="round" />
      <circle cx="24" cy="31" r="3.4" fill="#fff" />
      <path d="M24 33v5" stroke="#fff" stroke-width="3" stroke-linecap="round" />
    </g>
  ),
  smile: () => (
    <g>
      <circle cx="24" cy="24" r="19" fill="#F5B301" />
      <circle cx="17" cy="20" r="2.6" fill="#2B2540" />
      <circle cx="31" cy="20" r="2.6" fill="#2B2540" />
      <path d="M14 28c3 7 17 7 20 0" fill="none" stroke="#2B2540" stroke-width="3" stroke-linecap="round" />
    </g>
  ),
  locked: () => (
    <g>
      <rect x="9" y="22" width="30" height="21" rx="5" fill="#9AA6C4" />
      <path d="M15 22v-6a9 9 0 0 1 18 0v6" fill="none" stroke="#7683A6" stroke-width="4" stroke-linecap="round" />
    </g>
  ),
};

export const ICON_NAMES = { line: Object.keys(LINE), art: Object.keys(ART) };

export interface IconProps {
  name: string;
  size?: number;
  class?: string;
  title?: string;
}

/** Иконка. Без title она считается декоративной (текст рядом уже всё объясняет). */
export function Icon({ name, size = 24, class: cls, title }: IconProps) {
  const art = ART[name] as Draw | undefined;
  const line = LINE[name] as Draw | undefined;
  const draw = art ?? line ?? ART.star;
  const view = art || !line ? 48 : 24;
  return (
    <svg
      class={`icon ${cls ?? ''}`}
      width={size}
      height={size}
      viewBox={`0 0 ${view} ${view}`}
      role={title ? 'img' : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : 'true'}
      focusable="false"
    >
      {draw()}
    </svg>
  );
}
