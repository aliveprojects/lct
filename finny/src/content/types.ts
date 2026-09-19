import type { Direction, NeedTag, PetStat, Topic } from '../domain/types';

export interface Item {
  id: string;
  name: string;
  kind: 'must' | 'want';
  need?: NeedTag;
  price: number;
  icon: string;
  effects: Partial<Record<PetStat, number>>;
  accessory?: string;
  blurb: string;
  note: string;
}

export interface GoalDef {
  id: string;
  title: string;
  icon: string;
  cost: number;
  blurb: string;
}

export interface EventDef {
  id: string;
  period: number;
  title: string;
  /** Короткая строка для баннера на главном экране. */
  short: string;
  text: string;
  cost: number;
  effects: Partial<Record<PetStat, number>>;
  tip: string;
}

export interface GlossaryEntry {
  id: string;
  term: string;
  short: string;
  example?: string;
}

export interface Competency {
  id: number;
  text: string;
}

export interface SpeciesDef {
  id: string;
  name: string;
}
export interface ColorDef {
  id: string;
  name: string;
  body: string;
  belly: string;
  dark: string;
}
export interface AccessoryDef {
  id: string;
  name: string;
  atStart: boolean;
}
export interface StageDef {
  id: 1 | 2 | 3 | 4;
  name: string;
  blurb: string;
}
export interface PetOptions {
  species: SpeciesDef[];
  colors: ColorDef[];
  accessories: AccessoryDef[];
  stages: StageDef[];
  /** Имя питомца-талисмана: подставляется по умолчанию, ребёнок может ввести своё. */
  defaultPetName: string;
  petNames: string[];
  playerNames: string[];
}

export interface CustomGoalOptions {
  presets: { icon: string; title: string }[];
}

// ---------- Задания ----------

export interface TaskBase {
  id: string;
  topic: Topic;
  title: string;
  skill: string;
  competencies: number[];
  unlockPeriod: number;
  intro: string;
  rewards: { '1': number; '2': number; '3': number };
  learn: string;
}

export interface SortTask extends TaskBase {
  type: 'sort';
  groups: { id: 'need' | 'want'; label: string; hint: string }[];
  items: { id: string; label: string; icon: string; group: 'need' | 'want'; why: string }[];
}

export interface CartTask extends TaskBase {
  type: 'cart';
  budget: number;
  minLeft: number;
  items: { id: string; label: string; icon: string; price: number; need: boolean; why: string }[];
}

export interface AllocateTask extends TaskBase {
  type: 'allocate';
  total: number;
  step: number;
  buckets: { id: Direction; label: string; hint: string }[];
  rules: { id: string; bucket: Direction; op: 'min' | 'max'; value: number; ok: string; fail: string }[];
}

export interface CalcTask extends TaskBase {
  type: 'calc';
  questions: { id: string; text: string; answer: number; unit: string; explain: string }[];
}

export interface SimTask extends TaskBase {
  type: 'sim';
  goalCost: number;
  start: number;
  freePerWeek: number;
  step: number;
  targetWeeks: number;
  texts: { none: string; slow: string; good: string; great: string };
}

export interface StoryEnd {
  stars: 1 | 2 | 3;
  consequence: string;
  explain: string;
  recovery?: string;
}
export interface StoryNode {
  text: string;
  choices: { text: string; next?: string; end?: StoryEnd }[];
}
export interface StoryTask extends TaskBase {
  type: 'story';
  start: string;
  nodes: Record<string, StoryNode>;
}

export type TaskDef = SortTask | CartTask | AllocateTask | CalcTask | SimTask | StoryTask;

export interface Content {
  items: Item[];
  goals: GoalDef[];
  customGoal: CustomGoalOptions;
  tasks: TaskDef[];
  events: EventDef[];
  glossary: GlossaryEntry[];
  competencies: Competency[];
  pet: PetOptions;
}
