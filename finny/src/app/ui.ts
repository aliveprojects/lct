import type { ComponentChildren } from 'preact';
import { useEffect, useState } from 'preact/hooks';
import type { Report } from '../domain/types';

export interface ConfirmOptions {
  title: string;
  body?: ComponentChildren;
  confirmLabel: string;
  cancelLabel?: string;
  /** careful — действие заметно меняет прогресс: подсвечиваем аккуратнее. */
  tone?: 'normal' | 'careful';
  icon?: string;
}

export type Overlay =
  | { id: number; kind: 'confirm'; opts: ConfirmOptions; resolve: (ok: boolean) => void }
  | { id: number; kind: 'report'; report: Report; resolve: () => void }
  | { id: number; kind: 'sheet'; title?: string; render: (close: () => void) => ComponentChildren; resolve: () => void };

let seq = 1;
let stack: Overlay[] = [];
let toastText: { id: number; text: string } | null = null;
let toastTimer: ReturnType<typeof setTimeout> | undefined;
const listeners = new Set<() => void>();
const emit = () => listeners.forEach((l) => l());

function push(o: Overlay): void {
  stack = [...stack, o];
  emit();
}
function remove(id: number): void {
  stack = stack.filter((o) => o.id !== id);
  emit();
}

export const ui = {
  confirm(opts: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      const id = seq++;
      push({
        id,
        kind: 'confirm',
        opts,
        resolve: (ok) => {
          remove(id);
          resolve(ok);
        },
      });
    });
  },
  /** Показывает «что изменилось и почему». */
  report(report: Report): Promise<void> {
    return new Promise((resolve) => {
      const id = seq++;
      push({
        id,
        kind: 'report',
        report,
        resolve: () => {
          remove(id);
          resolve();
        },
      });
    });
  },
  sheet(render: (close: () => void) => ComponentChildren, title?: string): Promise<void> {
    return new Promise((resolve) => {
      const id = seq++;
      const close = () => {
        remove(id);
        resolve();
      };
      push({ id, kind: 'sheet', title, render, resolve: close });
    });
  },
  toast(text: string): void {
    toastText = { id: seq++, text };
    emit();
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toastText = null;
      emit();
    }, 3200);
  },
  /** Закрывает верхнее окно (для кнопки «назад» Android). Возвращает true, если было что закрывать. */
  closeTop(): boolean {
    const top = stack[stack.length - 1];
    if (!top) return false;
    if (top.kind === 'confirm') top.resolve(false);
    else top.resolve();
    return true;
  },
  hasOverlay: (): boolean => stack.length > 0,
};

export function useOverlays(): { stack: Overlay[]; toast: { id: number; text: string } | null } {
  const [, force] = useState(0);
  useEffect(() => {
    const l = () => force((n) => n + 1);
    listeners.add(l);
    return () => void listeners.delete(l);
  }, []);
  return { stack, toast: toastText };
}
