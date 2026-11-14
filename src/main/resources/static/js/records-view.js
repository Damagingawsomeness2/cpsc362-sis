// Views of academic records shared by the portals: profile facts, seats, the weekly
// timetable, academic history and the printable transcript.

import { badge, el, formatDate, formatLevel, formatTime, table } from './api.js';

const DAYS = [['M', 'Mon'], ['Tu', 'Tue'], ['W', 'Wed'], ['Th', 'Thu'], ['F', 'Fri'], ['Sa', 'Sat'], ['Su', 'Sun']];
const PIXELS_PER_MINUTE = 48 / 60; // matches --hour in sis.css

export function fact(label, value) {
  return el('div', {}, el('dt', {}, label), el('dd', {}, value));
}

export function profileFacts(profile) {
  return el('dl', { class: 'facts' },
    fact('Name', profile.name),
    fact('CWID', profile.cwid),
    fact('Major', profile.majorName ?? 'Undeclared'),
    fact('Grade level', formatLevel(profile.gradeLevel)),
    fact('Units completed', profile.unitsCompleted),
    profile.accessEnabled ? null : fact('Access', badge('Revoked', 'bad')));
}

export function seats(section) {
  const state = section.openSeats === 0 ? 'full'
    : section.openSeats <= Math.max(2, Math.floor(section.capacity / 10)) ? 'few' : 'open';
  const fill = el('span');
  fill.style.width = `${Math.round((section.enrolled / section.capacity) * 100)}%`;
  return el('div', { class: `seats ${state}` },
    el('span', { class: 'meter', 'aria-hidden': 'true' }, fill),
    el('span', { class: 'seats-label' },
      section.openSeats === 0 ? `Full (${section.capacity} seats)` : `${section.openSeats} of ${section.capacity} open`));
}

export function statusBadge(status) {
  return {
    ENROLLED: badge('Enrolled', 'neutral'),
    COMPLETED: badge('Completed', 'ok'),
    DROPPED: badge('Dropped', 'bad'),
  }[status];
}

/**
 * A week of classes as blocks on a grid. Purely visual (hidden from screen readers):
 * the table that follows it carries the same information.
 */
export function timetable(classes) {
  const timed = classes.filter((c) => c.days && c.startTime && c.endTime);
  if (!timed.length) return null;

  const daysOf = (c) => c.days.match(/Tu|Th|Sa|Su|M|W|F/g) ?? [];
  const minutes = (time) => { const [h, m] = time.split(':').map(Number); return h * 60 + m; };
  const shownDays = DAYS.filter(([code], index) => index < 5 || timed.some((c) => daysOf(c).includes(code)));
  const first = Math.floor(Math.min(8 * 60, ...timed.map((c) => minutes(c.startTime))) / 60) * 60;
  const last = Math.ceil(Math.max(17 * 60, ...timed.map((c) => minutes(c.endTime))) / 60) * 60;
  const tones = new Map(classes.map((c, index) => [c.courseCode, index % 6]));

  const grid = el('div', { class: 'timetable', 'aria-hidden': 'true' });
  grid.style.setProperty('--days', shownDays.length);
  grid.append(el('div', { class: 'corner' }), ...shownDays.map(([, name]) => el('div', { class: 'day-name' }, name)));

  const hours = el('div', { class: 'hours' });
  for (let minute = first; minute < last; minute += 60) {
    hours.append(el('span', {}, formatTime(`${minute / 60}:00`).replace(':00 ', ' ')));
  }
  grid.append(hours);

  for (const [code] of shownDays) {
    const column = el('div', { class: 'day-column' });
    column.style.height = `${(last - first) * PIXELS_PER_MINUTE}px`;
    for (const c of timed.filter((candidate) => daysOf(candidate).includes(code))) {
      const block = el('div', { class: `class-block tone-${tones.get(c.courseCode)}` },
        el('strong', {}, c.courseCode),
        el('span', {}, `${formatTime(c.startTime)}–${formatTime(c.endTime)}`),
        c.room ? el('span', {}, c.room) : null);
      block.style.top = `${(minutes(c.startTime) - first) * PIXELS_PER_MINUTE}px`;
      block.style.height = `${(minutes(c.endTime) - minutes(c.startTime)) * PIXELS_PER_MINUTE}px`;
      column.append(block);
    }
    grid.append(column);
  }
  return el('div', { class: 'timetable-wrap' }, grid);
}

/** A student's completed classes, newest term first. */
export function history(transcript) {
  if (!transcript.terms.length) {
    return [el('p', { class: 'empty' }, 'No completed classes yet. Grades appear here once professors post them.')];
  }
  return [
    el('dl', { class: 'facts' },
      fact('Cumulative GPA', transcript.gpa.toFixed(2)),
      fact('Units earned', transcript.unitsEarned),
      fact('Units attempted', transcript.unitsAttempted)),
    ...[...transcript.terms].reverse().flatMap((term) => [
      el('h3', {}, term.termName),
      table(['Course', 'Title', 'Units', 'Grade'], term.courses.map((line) =>
        [el('span', { class: 'course-code' }, line.courseCode), line.courseTitle, line.units, line.grade])),
      el('p', { class: 'muted' }, `Term GPA ${term.termGpa.toFixed(2)}`),
    ]),
  ];
}

/** The official transcript: oldest term first, set as a printable document. */
export function transcriptDocument(transcript, issuedOn) {
  const student = transcript.student;
  return el('article', { class: 'record' },
    el('header', { class: 'record-head' },
      el('h2', {}, 'Academic transcript'),
      el('p', { class: 'record-office' }, 'Titan SIS, Office of the Registrar'),
      el('dl', { class: 'facts' },
        fact('Student', student.name),
        fact('CWID', student.cwid),
        fact('Major', student.majorName ?? 'Undeclared'),
        fact('Grade level', formatLevel(student.gradeLevel)),
        fact('Issued', formatDate(issuedOn)))),
    transcript.terms.length ? null : el('p', {}, 'No completed classes.'),
    ...transcript.terms.flatMap((term) => [
      el('h3', {}, term.termName),
      table(['Course', 'Title', 'Units', 'Grade'], term.courses.map((line) =>
        [line.courseCode, line.courseTitle, line.units, line.grade])),
      el('p', { class: 'record-term-gpa' }, `Term GPA ${term.termGpa.toFixed(2)}`),
    ]),
    el('p', { class: 'record-totals' },
      `Units attempted ${transcript.unitsAttempted}. Units earned ${transcript.unitsEarned}. `
      + `Cumulative GPA ${transcript.gpa.toFixed(2)}.`));
}

export function today() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
