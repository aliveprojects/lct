import { useEffect } from 'preact/hooks';
import { Icon } from '../ui/icons/Icon';
import { Button, ChangeList, Confetti, SheetFrame } from '../ui/components/kit';
import { nav } from './nav';
import { useAppState } from './store';
import { useOverlays } from './ui';
import type { Overlay } from './ui';

function ReportView({ o }: { o: Extract<Overlay, { kind: 'report' }> }) {
  const { report } = o;
  const app = useAppState();
  const celebrate = report.tone === 'good' && app.settings.animations && report.changes.some((c) => c.kind === 'growth' || c.kind === 'savings' || (c.kind === 'wallet' && c.delta > 0));
  return (
    <SheetFrame title={report.title} onClose={o.resolve} tone={report.tone}>
      {celebrate && <Confetti />}
      <div class="stack">
        <ChangeList changes={report.changes} />
        <p class="explain">
          <Icon name="bulb" size={26} />
          <span>{report.explain}</span>
        </p>
        <div class="row row--wrap">
          {report.next && (
            <Button
              block
              onClick={() => {
                o.resolve();
                nav.go(report.next!.route);
              }}
            >
              {report.next.label}
            </Button>
          )}
          <Button block variant={report.next ? 'secondary' : 'primary'} onClick={o.resolve}>
            Понятно
          </Button>
        </div>
      </div>
    </SheetFrame>
  );
}

export function OverlayHost() {
  const { stack, toast } = useOverlays();
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && stack.length) {
        const top = stack[stack.length - 1];
        if (top.kind === 'confirm') top.resolve(false);
        else top.resolve();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [stack]);

  return (
    <>
      {stack.map((o) => {
        if (o.kind === 'report') return <ReportView key={o.id} o={o} />;
        if (o.kind === 'confirm')
          return (
            <SheetFrame key={o.id} title={o.opts.title} onClose={() => o.resolve(false)} tone={o.opts.tone === 'careful' ? 'care' : undefined}>
              <div class="stack">
                {o.opts.icon && (
                  <div class="center">
                    <Icon name={o.opts.icon} size={56} />
                  </div>
                )}
                {o.opts.body}
                <div class="row row--wrap">
                  <Button block variant="secondary" onClick={() => o.resolve(false)}>
                    {o.opts.cancelLabel ?? 'Отмена'}
                  </Button>
                  <Button block variant={o.opts.tone === 'careful' ? 'gold' : 'primary'} onClick={() => o.resolve(true)}>
                    {o.opts.confirmLabel}
                  </Button>
                </div>
              </div>
            </SheetFrame>
          );
        return (
          <SheetFrame key={o.id} title={o.title} onClose={o.resolve}>
            {o.render(o.resolve)}
          </SheetFrame>
        );
      })}
      {toast && (
        <div class="toast" role="status" aria-live="polite" key={toast.id}>
          {toast.text}
        </div>
      )}
    </>
  );
}
