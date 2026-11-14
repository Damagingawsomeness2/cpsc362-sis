// Student portal: register for sections, see the week's schedule, and review grades.

import { api, el, formatDate, handleForm, notify, table } from './api.js';
import { run, startPortal } from './portal.js';
import { history, profileFacts, seats, timetable } from './records-view.js';

const termSelect = document.getElementById('term');
const searchForm = document.getElementById('section-search');
let terms = [];

const user = startPortal('STUDENT', {
  register: showRegister,
  schedule: showSchedule,
  history: showHistory,
});
if (user) run(showProfile);

termSelect.addEventListener('change', () => run(showSections));
handleForm(searchForm, showSections);

async function showProfile() {
  document.getElementById('profile').replaceChildren(profileFacts(await api('/api/student/profile')));
}

/** Terms newest first, with the one open for registration selected. */
async function loadTerms() {
  if (!terms.length) {
    terms = await api('/api/catalog/terms');
    termSelect.replaceChildren(...[...terms].reverse().map((term) =>
      el('option', { value: term.id }, term.addDropOpen ? `${term.name} (open for registration)` : term.name)));
    const open = terms.find((term) => term.addDropOpen);
    if (open) termSelect.value = String(open.id);
  }
  return terms;
}

async function showRegister() {
  await loadTerms();
  await showSections();
}

async function showSections() {
  const term = terms.find((candidate) => String(candidate.id) === termSelect.value);
  if (!term) return;
  const search = searchForm.elements.namedItem('search').value.trim();
  const query = search ? `?search=${encodeURIComponent(search)}` : '';
  const sections = await api(`/api/catalog/terms/${term.id}/sections${query}`);

  document.getElementById('term-note').textContent = term.addDropOpen
    ? `You can add or drop ${term.name} classes through ${formatDate(term.addDropDeadline)}.`
    : `Registration for ${term.name} closed on ${formatDate(term.addDropDeadline)}.`;

  const results = document.getElementById('sections');
  if (!sections.length) {
    results.replaceChildren(el('p', { class: 'empty' }, search
      ? `No ${term.name} sections match “${search}”. Try a course code such as CPSC 335, or part of a title.`
      : `No sections are scheduled for ${term.name} yet.`));
    return;
  }
  results.replaceChildren(table(['Course', 'Title', 'Section', 'Meets', 'Room', 'Instructor', 'Seats', ''],
    sections.map((section) => [
      el('span', { class: 'course-code' }, section.courseCode),
      section.courseTitle,
      section.sectionNumber,
      el('span', { class: 'nowrap' }, section.meets),
      el('span', { class: 'nowrap' }, section.room ?? ''),
      el('span', { class: 'nowrap' }, section.professorName),
      seats(section),
      enrollButton(section, term),
    ])));
}

function enrollButton(section, term) {
  const full = section.openSeats === 0;
  return el('button', {
    class: 'button small',
    type: 'button',
    disabled: !term.addDropOpen || full,
    'aria-label': `Enroll in ${section.courseCode} section ${section.sectionNumber}`,
    onclick: async (event) => {
      const button = event.currentTarget;
      button.disabled = true;
      try {
        await api('/api/student/enrollments', { method: 'POST', body: { sectionId: section.id } });
        notify(`Enrolled in ${section.courseCode} section ${section.sectionNumber}.`);
        await showSections();
      } catch (failure) {
        notify(failure.message, 'error');
        button.disabled = false;
      }
    },
  }, full ? 'Full' : 'Enroll');
}

async function showSchedule() {
  const [classes] = await Promise.all([api('/api/student/schedule'), loadTerms()]);
  const page = document.getElementById('schedule');
  if (!classes.length) {
    page.replaceChildren(el('div', { class: 'panel' },
      el('h2', {}, 'My schedule'),
      el('p', { class: 'empty' }, 'You are not registered for any classes. Find open sections on the Register tab.')));
    return;
  }

  const termIds = [...new Set(classes.map((c) => c.termId))];
  page.replaceChildren(...termIds.map((termId) => {
    const term = terms.find((candidate) => candidate.id === termId);
    const termClasses = classes.filter((c) => c.termId === termId);
    const units = termClasses.reduce((sum, c) => sum + c.units, 0);
    return el('div', { class: 'panel' },
      el('h2', {}, `${term.name} schedule`),
      el('p', { class: 'lede' },
        `${termClasses.length} ${termClasses.length === 1 ? 'class' : 'classes'}, ${units} units. `,
        term.addDropOpen
          ? `You can drop classes through ${formatDate(term.addDropDeadline)}.`
          : `The last day to drop was ${formatDate(term.addDropDeadline)}. To leave a class now, ask your professor.`),
      timetable(termClasses),
      table(['Course', 'Title', 'Units', 'Section', 'Meets', 'Room', 'Instructor', ''], termClasses.map((c) => [
        el('span', { class: 'course-code' }, c.courseCode),
        c.courseTitle,
        c.units,
        c.sectionNumber,
        el('span', { class: 'nowrap' }, c.meets),
        el('span', { class: 'nowrap' }, c.room ?? ''),
        el('span', { class: 'nowrap' }, c.professorName),
        term.addDropOpen ? dropButton(c) : null,
      ])));
  }));
}

function dropButton(enrolled) {
  return el('button', {
    class: 'button small danger',
    type: 'button',
    'aria-label': `Drop ${enrolled.courseCode}`,
    onclick: async () => {
      if (!confirm(`Drop ${enrolled.courseCode} ${enrolled.courseTitle}? Your seat goes to the next student who registers.`)) return;
      try {
        await api(`/api/student/enrollments/${enrolled.sectionId}`, { method: 'DELETE' });
        notify(`Dropped ${enrolled.courseCode}.`);
        await showSchedule();
      } catch (failure) {
        notify(failure.message, 'error');
      }
    },
  }, 'Drop');
}

async function showHistory() {
  const transcript = await api('/api/student/history');
  document.getElementById('history').replaceChildren(
    el('div', { class: 'panel history' }, el('h2', {}, 'Academic history'), ...history(transcript)));
}
