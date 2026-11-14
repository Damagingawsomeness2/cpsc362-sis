// Professor portal: class rolls, final grades, dropping students, and student lookups.

import { api, el, formatLevel, handleForm, notify, table } from './api.js';
import { run, startPortal } from './portal.js';
import { history, profileFacts, statusBadge } from './records-view.js';

const GRADES = ['A+', 'A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D+', 'D', 'D-', 'F'];

startPortal('PROFESSOR', {
  classes: showClasses,
  lookup: () => {},
});
handleForm(document.getElementById('lookup-form'), lookUp);

async function showClasses() {
  const sections = await api('/api/professor/sections');
  const list = document.getElementById('my-sections');
  if (!sections.length) {
    list.replaceChildren(el('p', { class: 'empty' },
      'You are not teaching any sections yet. The registrar assigns instructors when building the class schedule.'));
    return;
  }
  list.replaceChildren(table(['Term', 'Course', 'Section', 'Meets', 'Room', 'Enrolled', ''], sections.map((section) => [
    section.termName,
    el('span', {}, el('span', { class: 'course-code' }, section.courseCode), ' ', section.courseTitle),
    section.sectionNumber,
    el('span', { class: 'nowrap' }, section.meets),
    section.room ?? '',
    `${section.enrolled} of ${section.capacity}`,
    el('button', {
      class: 'button small secondary', type: 'button',
      'aria-label': `Open the roll for ${section.courseCode} section ${section.sectionNumber}, ${section.termName}`,
      onclick: () => run(() => openRoll(section)),
    }, 'Open roll'),
  ])));
}

async function openRoll(section) {
  const roll = await api(`/api/professor/sections/${section.id}/roll`);
  const enrolled = roll.filter((entry) => entry.status === 'ENROLLED').length;
  const panel = document.getElementById('roll');
  const heading = el('h2', { tabindex: '-1' }, `${section.courseCode} section ${section.sectionNumber}, ${section.termName}`);
  const intro = el('p', { class: 'lede' },
    `${section.courseTitle}. ${section.meets}${section.room ? ` in ${section.room}` : ''}. `,
    `${enrolled} enrolled, ${roll.length - enrolled} graded.`);

  if (!roll.length) {
    panel.replaceChildren(heading, intro, el('p', { class: 'empty' }, 'Nobody is enrolled in this section yet.'));
  } else {
    const form = el('form', {},
      table(['CWID', 'Name', 'Grade level', 'Status', 'Final grade', ''], roll.map((entry) => [
        entry.cwid,
        entry.name,
        formatLevel(entry.gradeLevel),
        statusBadge(entry.status),
        gradeSelect(entry),
        entry.status === 'ENROLLED' ? dropButton(section, entry) : null,
      ])),
      el('div', { class: 'form-actions' }, el('button', { class: 'button', type: 'submit' }, 'Save grades')));
    handleForm(form, () => saveGrades(section, form));
    panel.replaceChildren(heading, intro, form);
  }
  panel.hidden = false;
  heading.focus();
}

function gradeSelect(entry) {
  return el('select', {
    'aria-label': `Final grade for ${entry.name}`,
    dataset: { studentId: entry.studentId, posted: entry.grade ?? '' },
  },
  el('option', { value: '' }, 'Not graded'),
  GRADES.map((grade) => el('option', { value: grade, selected: grade === entry.grade }, grade)));
}

async function saveGrades(section, form) {
  const grades = [...form.querySelectorAll('select[data-student-id]')]
    .filter((select) => select.value && select.value !== select.dataset.posted)
    .map((select) => ({ studentId: Number(select.dataset.studentId), grade: select.value }));
  if (!grades.length) {
    notify('Choose a new grade for at least one student, then save.', 'error');
    return;
  }
  await api(`/api/professor/sections/${section.id}/grades`, { method: 'PUT', body: { grades } });
  notify(`Saved ${grades.length} ${grades.length === 1 ? 'grade' : 'grades'}. Units and grade levels are updated.`);
  await openRoll(section);
}

function dropButton(section, entry) {
  return el('button', {
    class: 'button small danger',
    type: 'button',
    'aria-label': `Drop ${entry.name}`,
    onclick: async () => {
      if (!confirm(`Drop ${entry.name} from ${section.courseCode} section ${section.sectionNumber}?`)) return;
      try {
        await api(`/api/professor/sections/${section.id}/students/${entry.studentId}`, { method: 'DELETE' });
        notify(`Dropped ${entry.name} from ${section.courseCode}.`);
        await Promise.all([openRoll(section), showClasses()]);
      } catch (failure) {
        notify(failure.message, 'error');
      }
    },
  }, 'Drop');
}

async function lookUp({ cwid }) {
  const path = `/api/professor/students/${encodeURIComponent(cwid.trim())}`;
  const [profile, schedule, transcript] = await Promise.all([
    api(path), api(`${path}/schedule`), api(`${path}/history`),
  ]);
  document.getElementById('lookup-result').replaceChildren(
    el('div', { class: 'panel' }, el('h2', {}, profile.name), profileFacts(profile)),
    el('div', { class: 'panel' },
      el('h2', {}, 'Current schedule'),
      schedule.length
        ? table(['Term', 'Course', 'Title', 'Section', 'Meets', 'Instructor'], schedule.map((c) => [
          c.termName, el('span', { class: 'course-code' }, c.courseCode), c.courseTitle, c.sectionNumber,
          el('span', { class: 'nowrap' }, c.meets), c.professorName]))
        : el('p', { class: 'empty' }, `${profile.name} is not registered for any classes.`)),
    el('div', { class: 'panel history' }, el('h2', {}, 'Academic history'), ...history(transcript)));
}
