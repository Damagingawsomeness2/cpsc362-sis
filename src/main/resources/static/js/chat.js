// The Chatbot Assistant panel, available in every portal.

import { api, el } from './api.js';

export function startAssistant() {
  const log = el('div', { class: 'assistant-log', role: 'log', 'aria-live': 'polite' },
    message('assistant', 'Ask me about a course, its prerequisites or open seats, or about the add/drop deadline.'));
  const input = el('input', {
    name: 'message', maxlength: 500, autocomplete: 'off', 'aria-label': 'Your question',
    placeholder: 'What do I need before CPSC 362?',
  });
  const form = el('form', { onsubmit: ask }, input, el('button', { class: 'button', type: 'submit' }, 'Ask'));
  const panel = el('section', { class: 'assistant-panel', id: 'assistant', 'aria-label': 'Assistant', hidden: true },
    el('header', {},
      el('h2', {}, 'Assistant'),
      el('button', { class: 'icon-button', type: 'button', 'aria-label': 'Close the assistant', onclick: close }, '×')),
    log, form);
  const toggle = el('button', {
    class: 'button small assistant-toggle', type: 'button', 'aria-expanded': 'false', 'aria-controls': 'assistant',
    onclick: () => (panel.hidden ? open() : close()),
  }, 'Ask the assistant');
  // The button lives in the header so it never covers a row's buttons; the panel opens beneath it.
  document.querySelector('.account').prepend(toggle);
  document.body.append(panel);

  function open() {
    panel.hidden = false;
    toggle.setAttribute('aria-expanded', 'true');
    input.focus();
  }

  function close() {
    panel.hidden = true;
    toggle.setAttribute('aria-expanded', 'false');
    toggle.focus();
  }

  async function ask(event) {
    event.preventDefault();
    const question = input.value.trim();
    if (!question) return;
    input.value = '';
    log.append(message('user', question));
    try {
      const { reply } = await api('/api/chat', { method: 'POST', body: { message: question } });
      log.append(message('assistant', reply));
    } catch (failure) {
      log.append(message('assistant', failure.message));
    }
    log.scrollTop = log.scrollHeight;
  }
}

function message(sender, text) {
  return el('p', { class: `message ${sender}` }, text);
}
