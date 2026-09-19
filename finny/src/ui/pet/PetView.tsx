import { useAppState } from '../../app/store';
import { content } from '../../content';
import { petExpression } from '../../domain/profile';
import type { ExpressionCode } from '../../domain/profile';
import type { PetAppearance, PetStat, Profile, Stage } from '../../domain/types';
import { PetSvg } from './PetSvg';

interface Props {
  appearance: PetAppearance;
  stage: Stage;
  stats?: Record<PetStat, number>;
  expression?: ExpressionCode;
  size?: number | string;
  uid?: string;
  label?: string;
}

export function PetView({ appearance, stage, stats, expression, size, uid, label }: Props) {
  const app = useAppState();
  const color = content.pet.colors.find((c) => c.id === appearance.color) ?? content.pet.colors[0];
  const expr = expression ?? (stats ? petExpression(stats).code : 'content');
  return (
    <PetSvg
      species={appearance.species}
      color={color}
      accessory={appearance.accessory}
      stage={stage}
      expression={expr}
      size={size}
      uid={uid}
      label={label}
      animate={app.settings.animations}
    />
  );
}

export const PetOf = ({ profile, size, uid }: { profile: Profile; size?: number | string; uid?: string }) => (
  <PetView appearance={profile.pet.appearance} stage={profile.pet.stage} stats={profile.pet.stats} size={size} uid={uid} label={`${profile.pet.name}: ${petExpression(profile.pet.stats).label}`} />
);
