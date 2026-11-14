// Talks to the SIS REST API and holds the small DOM helpers every page uses.
// Text always goes into the page as text nodes (never innerHTML), so data cannot inject markup.

const TOKEN_KEY = 'sis.token';
const USER_KEY = 'sis.user';
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/* Session: kept in sessionStorage, so closing the tab signs the browser out. */

export function session() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function saveSession(login) {
  sessionStorage.setItem(TOKEN_KEY, login.token);
  sessionStorage.setItem(USER_KEY, JSON.stringify(login.user));
}

export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export function portalFor(role) {
  return { STUDENT: '/student.html', PROFESSOR: '/professor.html', ADMIN: '/admin.html' }[role] ?? '/';
}

/* API calls */

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export async function api(path, { method = 'GET', body } = {}) {
  const token = sessionStorage.getItem(TOKEN_KEY);
  const headers = { Accept: 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  if (response.status === 401 && token) {
    clearSession();
    location.replace('/');
    throw new ApiError(401, 'Your session has ended. Please sign in again.');
  }
  if (!response.ok) {
    let detail = `The request failed (${response.status}).`;
    try {
      const problem = await response.json();
      detail = problem.detail || problem.title || detail;
    } catch {
      // Not JSON; keep the generic message.
    }
    throw new ApiError(response.status, detail);
  }
  return response.status === 204 ? null : response.json();
}

/* DOM helpers */

export function el(tag, attributes = {}, ...children) {
  const node = document.createElement(tag);
  for (const [name, value] of Object.entries(attributes)) {
    if (value === false || value === null || value === undefined) continue;
    if (name === 'class') node.className = value;
    else if (name === 'dataset') Object.assign(node.dataset, value);
    else if (name.startsWith('on')) node.addEventListener(name.slice(2), value);
    else node.setAttribute(name, value === true ? '' : String(value));
  }
  for (const child of children.flat()) {
    if (child === null || child === undefined || child === false) continue;
    node.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
  return node;
}

/** A scrollable table. An empty heading becomes a hidden "Actions" label for screen readers. */
export function table(headings, rows) {
  return el('div', { class: 'table-wrap' },
    el('table', {},
      el('thead', {}, el('tr', {}, headings.map((heading) =>
        el('th', { scope: 'col' }, heading === '' ? el('span', { class: 'visually-hidden' }, 'Actions') : heading)))),
      el('tbody', {}, rows.map((cells) => el('tr', {}, cells.map((cell) => el('td', {}, cell)))))));
}

export function badge(text, tone) {
  return el('span', { class: `badge ${tone}` }, text);
}

/** Wires a form to an async action; the action receives the form's fields as an object. */
export function handleForm(form, action) {
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const button = event.submitter;
    if (button) button.disabled = true;
    try {
      await action(Object.fromEntries(new FormData(form)), event);
    } catch (failure) {
      notify(failure.message, 'error');
    } finally {
      if (button) button.disabled = false;
    }
  });
}

const notice = el('div', { class: 'notice', role: 'status', 'aria-live': 'polite', hidden: true });
document.body.append(notice);
let noticeTimer;

export function notify(message, kind = 'success') {
  notice.textContent = message;
  notice.classList.toggle('error', kind === 'error');
  notice.setAttribute('aria-live', kind === 'error' ? 'assertive' : 'polite');
  notice.hidden = false;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => { notice.hidden = true; }, kind === 'error' ? 9000 : 4500);
}

/* Formatting */

/** "2027-01-31" to "Jan 31, 2027", matching the server's messages. */
export function formatDate(isoDate) {
  if (!isoDate) return '';
  const [year, month, day] = isoDate.split('-').map(Number);
  return `${MONTHS[month - 1]} ${day}, ${year}`;
}

/** "13:00:00" to "1:00 PM". */
export function formatTime(isoTime) {
  const [hours, minutes] = isoTime.split(':').map(Number);
  return `${hours % 12 || 12}:${String(minutes).padStart(2, '0')} ${hours < 12 ? 'AM' : 'PM'}`;
}

export function formatLevel(level) {
  return level ? level.charAt(0) + level.slice(1).toLowerCase() : '';
}

export function formatRole(role) {
  return { STUDENT: 'Student', PROFESSOR: 'Professor', ADMIN: 'Administrator' }[role] ?? role;
}

export function blankToNull(value) {
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : null;
}

/** "CPSC 131, math 170b" to ["CPSC 131", "math 170b"]; the server normalizes the codes. */
export function codeList(text) {
  return (text ?? '').split(',').map((code) => code.trim()).filter(Boolean);
}
