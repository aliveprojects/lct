import { describe, expect, it } from 'vitest';
import { TOPICS, content, validateContent } from '../src/content';

describe('Контент отделён от логики и целостен', () => {
  it('проходит проверку целостности', () => {
    expect(validateContent(content)).toEqual([]);
  });

  it('минимальный объём демонстрационного контента из ТЗ (п. 2.6)', () => {
    const must = content.items.filter((i) => i.kind === 'must');
    const want = content.items.filter((i) => i.kind === 'want');
    expect(content.items.length).toBeGreaterThanOrEqual(8);
    expect(must.length).toBeGreaterThan(0);
    expect(want.length).toBeGreaterThan(0);
    expect(content.goals.length).toBeGreaterThanOrEqual(3);
    expect(content.tasks.length).toBeGreaterThanOrEqual(6);
    const topics = new Set(content.tasks.map((t) => t.topic));
    expect(topics.size).toBe(3);
    expect(TOPICS.map((t) => t.id).sort()).toEqual([...topics].sort());
    expect(content.pet.stages.length).toBeGreaterThanOrEqual(3);
  });

  it('не менее 9 визуально различимых комбинаций питомца', () => {
    const starts = content.pet.accessories.filter((a) => a.atStart).length;
    expect(content.pet.species.length * content.pet.colors.length * starts).toBeGreaterThanOrEqual(9);
  });

  it('задания не ограничиваются выбором ответа из списка', () => {
    const types = new Set(content.tasks.map((t) => t.type));
    expect(types.size).toBeGreaterThanOrEqual(4);
    expect([...types].some((t) => t !== 'story')).toBe(true);
  });

  it('питомец-талисман Финни: имя по умолчанию есть в списке имён и годится для ввода', () => {
    expect(content.pet.defaultPetName).toBe('Финни');
    expect(content.pet.petNames).toContain('Финни');
    expect(content.pet.defaultPetName.length).toBeGreaterThanOrEqual(2);
    expect(content.pet.defaultPetName.length).toBeLessThanOrEqual(12);
  });

  it('у обязательных товаров есть еда и уход', () => {
    const needs = new Set(content.items.filter((i) => i.kind === 'must').map((i) => i.need));
    expect(needs.has('food')).toBe(true);
    expect(needs.has('care')).toBe(true);
  });

  it('валидатор действительно ловит ошибки', () => {
    const broken = { ...content, items: [...content.items, { ...content.items[0] }] };
    expect(validateContent(broken).some((e) => e.includes('повторяется'))).toBe(true);
  });
});
