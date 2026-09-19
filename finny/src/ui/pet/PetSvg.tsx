import type { JSX } from 'preact';
import type { ColorDef } from '../../content/types';
import type { ExpressionCode } from '../../domain/profile';
import type { Stage } from '../../domain/types';

// Питомец рисуется из параметров: вид × цвет × украшение × стадия × выражение.
// Все фигуры — собственные, векторные, без внешних файлов.

const INK = '#2B2540';
const PINK = '#F7B8C6';

export interface PetSvgProps {
  species: string;
  color: ColorDef;
  accessory: string;
  stage: Stage;
  expression: ExpressionCode;
  /** Уникальный суффикс для id градиентов, если на экране несколько питомцев. */
  uid?: string;
  animate?: boolean;
  size?: number | string;
  label?: string;
}

const STAGE_SCALE: Record<Stage, number> = { 1: 0.8, 2: 0.9, 3: 0.96, 4: 1 };

function BackParts({ species, c }: { species: string; c: ColorDef }) {
  switch (species) {
    case 'cat':
      return (
        <g>
          <path d="M136 160C170 162 174 128 152 122" stroke={c.dark} stroke-width="11" fill="none" stroke-linecap="round" />
          <path d="M60 68L52 26L94 50Z" fill={c.body} />
          <path d="M65 61L60 38L83 51Z" fill={PINK} />
          <path d="M140 68L148 26L106 50Z" fill={c.body} />
          <path d="M135 61L140 38L117 51Z" fill={PINK} />
        </g>
      );
    case 'dog':
      return (
        <g>
          <path d="M136 154C156 148 160 132 150 120" stroke={c.dark} stroke-width="10" fill="none" stroke-linecap="round" />
          <ellipse cx="54" cy="94" rx="15" ry="28" fill={c.dark} transform="rotate(10 54 94)" />
          <ellipse cx="146" cy="94" rx="15" ry="28" fill={c.dark} transform="rotate(-10 146 94)" />
        </g>
      );
    case 'bunny':
      return (
        <g>
          <circle cx="140" cy="166" r="11" fill="#fff" />
          <ellipse cx="76" cy="36" rx="13" ry="35" fill={c.body} transform="rotate(-8 76 36)" />
          <ellipse cx="76" cy="38" rx="6.5" ry="24" fill={PINK} transform="rotate(-8 76 36)" />
          <ellipse cx="124" cy="36" rx="13" ry="35" fill={c.body} transform="rotate(8 124 36)" />
          <ellipse cx="124" cy="38" rx="6.5" ry="24" fill={PINK} transform="rotate(8 124 36)" />
        </g>
      );
    default:
      // dino
      return (
        <g>
          <path d="M134 158C158 158 172 170 178 184C160 184 146 180 130 176Z" fill={c.body} />
          <path d="M150 162l4-9 5 10M162 168l5-8 4 10" fill={c.dark} />
          <path d="M78 56L86 36L95 52Z" fill={c.dark} />
          <path d="M93 50L100 28L107 50Z" fill={c.dark} />
          <path d="M105 52L114 36L122 56Z" fill={c.dark} />
        </g>
      );
  }
}

function FaceParts({ species, c }: { species: string; c: ColorDef }) {
  switch (species) {
    case 'cat':
      return (
        <g>
          <path d="M96 100L104 100L100 105Z" fill="#E86F8A" />
          <path d="M72 104L50 100M72 110L50 113M128 104L150 100M128 110L150 113" stroke={INK} stroke-width="2.2" stroke-linecap="round" opacity=".55" />
          <path d="M92 60l2 8M100 58v9M108 60l-2 8" stroke={c.dark} stroke-width="3" stroke-linecap="round" />
        </g>
      );
    case 'dog':
      return (
        <g>
          <ellipse cx="100" cy="108" rx="21" ry="15" fill={c.belly} />
          <ellipse cx="100" cy="99" rx="7.5" ry="5.5" fill={INK} />
          <ellipse cx="98" cy="97.500" rx="2.200" ry="1.300" fill="#fff" opacity=".7" />
        </g>
      );
    case 'bunny':
      return <ellipse cx="100" cy="101" rx="4" ry="3" fill="#E86F8A" />;
    default:
      return (
        <g>
          <ellipse cx="100" cy="106" rx="22" ry="14" fill={c.belly} />
          <circle cx="93" cy="101" r="2" fill={c.dark} />
          <circle cx="107" cy="101" r="2" fill={c.dark} />
        </g>
      );
  }
}

function Eyes({ expression }: { expression: ExpressionCode }) {
  const round = (x: number) => (
    <g class="pet-eye">
      <circle cx={x} cy="90" r="7.5" fill={INK} />
      <circle cx={x + 2.500} cy="87" r="2.600" fill="#fff" />
    </g>
  );
  if (expression === 'happy')
    return (
      <g stroke={INK} stroke-width="4" fill="none" stroke-linecap="round">
        <path d="M73 92Q82 81 91 92" />
        <path d="M109 92Q118 81 127 92" />
      </g>
    );
  if (expression === 'bored')
    return (
      <g stroke={INK} stroke-width="4" fill="none" stroke-linecap="round">
        <path d="M74 91Q82 96 90 91" />
        <path d="M110 91Q118 96 126 91" />
        <path d="M72 82l16 3M128 82l-16 3" stroke-width="3" opacity=".6" />
      </g>
    );
  return (
    <g>
      {round(82)}
      {round(118)}
      {expression === 'hungry' && (
        <path d="M72 80l15 4M128 80l-15 4" stroke={INK} stroke-width="3" stroke-linecap="round" opacity=".6" />
      )}
    </g>
  );
}

function Mouth({ expression }: { expression: ExpressionCode }) {
  switch (expression) {
    case 'happy':
      return (
        <g>
          <path d="M89 109Q100 128 111 109Z" fill={INK} />
          <path d="M94 117Q100 124 106 117Q100 113 94 117Z" fill="#F27C8E" />
        </g>
      );
    case 'hungry':
      return (
        <g>
          <ellipse cx="100" cy="117" rx="6.500" ry="7.500" fill={INK} />
          <ellipse cx="100" cy="121" rx="3.600" ry="2.800" fill="#F27C8E" />
        </g>
      );
    case 'bored':
      return <path d="M92 118H108" stroke={INK} stroke-width="3.500" stroke-linecap="round" />;
    case 'dirty':
      return <path d="M92 114Q100 120 108 114" stroke={INK} stroke-width="3.500" fill="none" stroke-linecap="round" />;
    default:
      return <path d="M91 113Q100 122 109 113" stroke={INK} stroke-width="3.500" fill="none" stroke-linecap="round" />;
  }
}

function Accessory({ id }: { id: string }) {
  switch (id) {
    case 'bow':
      return (
        <g transform="translate(140 60)">
          <path d="M0 0L-17 -11L-17 11Z" fill="#F2647A" />
          <path d="M0 0L17 -11L17 11Z" fill="#F2647A" />
          <circle r="5.500" fill="#D9425A" />
        </g>
      );
    case 'helmet':
      return (
        <g>
          <circle cx="100" cy="92" r="55" fill="rgba(170,215,255,.1)" stroke="#d7e9ff" stroke-width="4" />
          <path d="M62 66Q74 48 96 44" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round" opacity=".75" />
          <ellipse cx="100" cy="137" rx="44" ry="8.500" fill="#8fa4ff" stroke="#d7e9ff" stroke-width="3" />
          <circle cx="66" cy="137" r="3" fill="#ffe066" />
          <circle cx="134" cy="137" r="3" fill="#ffe066" />
        </g>
      );
    case 'glasses':
      return (
        <g fill="rgba(255,255,255,.35)" stroke={INK} stroke-width="3.200">
          <circle cx="82" cy="90" r="15" />
          <circle cx="118" cy="90" r="15" />
          <path d="M97 90H103" fill="none" />
        </g>
      );
    case 'hat':
      return (
        <g transform="translate(100 56)">
          <rect x="-19" y="-36" width="38" height="36" rx="4" fill="#3B3B6B" />
          <rect x="-19" y="-13" width="38" height="8" fill="#E4534B" />
          <ellipse cx="0" cy="0" rx="32" ry="7.500" fill="#4A4A80" />
        </g>
      );
    case 'scarf':
      return (
        <g>
          <path d="M62 128Q100 148 138 128L136 144Q100 164 64 144Z" fill="#E4534B" />
          <path d="M116 146l12 28h-15l-5-26Z" fill="#E4534B" />
          <path d="M76 136v12M90 141v12M104 141v12M118 137v12" stroke="#fff" stroke-width="3" opacity=".75" stroke-linecap="round" />
        </g>
      );
    case 'flower':
      return (
        <g transform="translate(66 60)">
          {[0, 72, 144, 216, 288].map((a) => (
            <circle cx={Math.cos((a * Math.PI) / 180) * 8} cy={Math.sin((a * Math.PI) / 180) * 8} r="6.500" fill="#F98CA0" />
          ))}
          <circle r="5" fill="#F5B301" />
        </g>
      );
    default:
      return null;
  }
}

const star = (cx: number, cy: number, r: number, fill: string): JSX.Element => {
  const pts: string[] = [];
  for (let i = 0; i < 10; i++) {
    const rad = i % 2 === 0 ? r : r * 0.45;
    const a = (Math.PI / 5) * i - Math.PI / 2;
    pts.push(`${(cx + Math.cos(a) * rad).toFixed(1)},${(cy + Math.sin(a) * rad).toFixed(1)}`);
  }
  return <polygon points={pts.join(' ')} fill={fill} stroke="#D18F00" stroke-width="1.500" stroke-linejoin="round" />;
};

export function PetSvg({ species, color, accessory, stage, expression, uid = 'p', animate = true, size = '100%', label }: PetSvgProps) {
  const c = color;
  const s = STAGE_SCALE[stage];
  const aura = `aura-${uid}`;
  return (
    <svg
      class={`pet ${animate ? 'pet--anim' : ''}`}
      viewBox="0 0 200 200"
      width={size}
      height={size}
      role="img"
      aria-label={label}
      focusable="false"
    >
      <defs>
        <radialGradient id={aura}>
          <stop offset="0" stop-color="#FFE07A" stop-opacity=".75" />
          <stop offset="1" stop-color="#FFE07A" stop-opacity="0" />
        </radialGradient>
      </defs>
      <g class="pet-bob">
      {stage === 4 && <circle cx="100" cy="112" r="92" fill={`url(#${aura})`} class="pet-aura" />}
      <ellipse cx="100" cy="188" rx="52" ry="6.500" fill="#050826" opacity=".4" />
      <g class="pet-body" transform={`translate(100 184) scale(${s}) translate(-100 -184)`}>
        {stage >= 3 && <path d="M62 122Q52 164 46 184Q100 198 154 184Q148 164 138 122Z" fill="#D9425A" />}
        <BackParts species={species} c={c} />
        <ellipse cx="100" cy="146" rx="38" ry="35" fill={c.body} />
        <ellipse cx="100" cy="153" rx="24" ry="22" fill={c.belly} />
        <ellipse cx="64" cy="148" rx="8" ry="15" fill={c.body} transform="rotate(18 64 148)" />
        <ellipse cx="136" cy="148" rx="8" ry="15" fill={c.body} transform="rotate(-18 136 148)" />
        <ellipse cx="76" cy="180" rx="16" ry="8.500" fill={c.dark} />
        <ellipse cx="124" cy="180" rx="16" ry="8.500" fill={c.dark} />
        {expression === 'hungry' && <path d="M84 152q4-5 8 0t8 0 8 0" stroke={c.dark} stroke-width="2.600" fill="none" stroke-linecap="round" opacity=".7" />}
        {stage >= 2 && star(100, 152, 11, '#F5B301')}
        <circle cx="100" cy="92" r="45" fill={c.body} />
        <circle cx="66" cy="106" r="8.500" fill="#FF8FA3" opacity=".45" />
        <circle cx="134" cy="106" r="8.500" fill="#FF8FA3" opacity=".45" />
        {expression === 'dirty' && (
          <g fill="#8E6B4A" opacity=".55">
            <ellipse cx="74" cy="76" rx="6" ry="4" />
            <ellipse cx="128" cy="72" rx="4.500" ry="3.200" />
            <ellipse cx="112" cy="140" rx="7" ry="4.500" />
            <ellipse cx="80" cy="158" rx="4.500" ry="3" />
          </g>
        )}
        <FaceParts species={species} c={c} />
        <Eyes expression={expression} />
        <Mouth expression={expression} />
        <Accessory id={accessory} />
        {expression === 'bored' && (
          <text x="142" y="62" font-size="20" font-weight="800" fill={INK} opacity=".55">
            z
          </text>
        )}
        {expression === 'happy' && (
          <g>
            {star(40, 60, 7, '#FFD24D')}
            {star(162, 52, 5.500, '#FFD24D')}
          </g>
        )}
      </g>
      {stage === 4 && (
        <g class="pet-sparkles">
          {star(28, 100, 7, '#FFD24D')}
          {star(176, 108, 6, '#FFD24D')}
          {star(160, 36, 5, '#FFD24D')}
        </g>
      )}
      </g>
    </svg>
  );
}
