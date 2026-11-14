// Shared setup for the student, professor and admin portals.

import { api, clearSession, notify, session } from './api.js';
import { startAssistant } from './chat.js';

/**
 * Checks the signed-in role, fills in the header, and wires the tabs. Each tab's loader runs
 * every time the tab opens, so it always shows fresh data. Returns the signed-in user.
 */
export function startPortal(role, loaders) {
  const user = session();
  if (!user || user.role !== role) {
    location.replace('/');
    return null;
  }
  document.querySelectorAll('[data-user-name]').forEach((node) => { node.textContent = user.displayName; });
  document.querySelector('[data-sign-out]').addEventListener('click', signOut);

  const tabs = [...document.querySelectorAll('[role="tab"]')];
  const select = (tab) => {
    for (const other of tabs) {
      const selected = other === tab;
      other.setAttribute('aria-selected', String(selected));
      other.tabIndex = selected ? 0 : -1;
      document.getElementById(other.getAttribute('aria-controls')).hidden = !selected;
    }
    const panelId = tab.getAttribute('aria-controls');
    history.replaceState(null, '', `#${panelId}`);
    run(loaders[panelId]);
  };
  tabs.forEach((tab, index) => {
    tab.addEventListener('click', () => select(tab));
    tab.addEventListener('keydown', (event) => {
      const step = { ArrowRight: 1, ArrowLeft: -1 }[event.key];
      if (!step) return;
      const next = tabs[(index + step + tabs.length) % tabs.length];
      next.focus();
      select(next);
    });
  });

  // A link or the back button can change the #tab in the address bar without reloading the page.
  const tabForHash = () => tabs.find((tab) => `#${tab.getAttribute('aria-controls')}` === location.hash);
  window.addEventListener('hashchange', () => {
    const tab = tabForHash();
    if (tab && tab.getAttribute('aria-selected') !== 'true') select(tab);
  });

  startAssistant();
  select(tabForHash() ?? tabs[0]);
  return user;
}

/** Runs a task and shows its failure, if any, as a notice. */
export function run(task) {
  return Promise.resolve().then(task).catch((failure) => notify(failure.message, 'error'));
}

async function signOut() {
  try {
    await api('/api/auth/logout', { method: 'POST' });
  } catch {
    // Forgetting the token locally still signs this browser out.
  }
  clearSession();
  location.replace('/');
}
