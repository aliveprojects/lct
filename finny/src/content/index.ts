import competencies from './competencies.json';
import customGoal from './custom-goal.json';
import events from './events.json';
import glossary from './glossary.json';
import goals from './goals.json';
import items from './items.json';
import pet from './pet.json';
import topics from './topics.json';
import type { Content, TaskDef } from './types';

// Каждый файл tasks/*.json — одно задание. Чтобы добавить новое, достаточно положить сюда JSON:
// код приложения менять не нужно (подхватывается автоматически).
const taskModules = import.meta.glob('./tasks/*.json', { eager: true, import: 'default' }) as Record<string, TaskDef>;

const TOPIC_ORDER = topics.map((t) => t.id);

export const TOPICS = topics as { id: 'budget' | 'saving' | 'purchases'; title: string; blurb: string; icon: string }[];

export const content: Content = {
  items: items as unknown as Content['items'],
  goals: goals as Content['goals'],
  customGoal: customGoal as Content['customGoal'],
  events: events as unknown as Content['events'],
  glossary: glossary as Content['glossary'],
  competencies: competencies as Content['competencies'],
  pet: pet as unknown as Content['pet'],
  tasks: Object.values(taskModules).sort(
    (a, b) => a.unlockPeriod - b.unlockPeriod || TOPIC_ORDER.indexOf(a.topic) - TOPIC_ORDER.indexOf(b.topic) || a.id.localeCompare(b.id),
  ),
};

export { validateContent } from './validate';
