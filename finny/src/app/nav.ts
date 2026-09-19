import { useEffect, useState } from 'preact/hooks';

// Простая навигация «стопкой»: у каждого экрана одинаковая кнопка «назад», а аппаратная кнопка Android
// ведёт себя так же. Маршруты: '/', '/plan', '/task/<id>' и т.п.

let stack: string[] = ['/'];
const listeners = new Set<() => void>();
const emit = () => listeners.forEach((l) => l());

export const currentRoute = (): string => stack[stack.length - 1];
export const canGoBack = (): boolean => stack.length > 1;

export const nav = {
  go(route: string): void {
    if (route === currentRoute()) return;
    // возврат на уже открытый раздел не раздувает стопку
    const at = stack.lastIndexOf(route);
    stack = at >= 0 && route !== '/' ? stack.slice(0, at + 1) : [...stack, route];
    emit();
  },
  replace(route: string): void {
    stack = [...stack.slice(0, -1), route];
    emit();
  },
  reset(route = '/'): void {
    stack = [route];
    emit();
  },
  back(): boolean {
    if (stack.length <= 1) return false;
    stack = stack.slice(0, -1);
    emit();
    return true;
  },
};

export function useRoute(): string {
  const [, force] = useState(0);
  useEffect(() => {
    const l = () => force((n) => n + 1);
    listeners.add(l);
    return () => void listeners.delete(l);
  }, []);
  return currentRoute();
}
