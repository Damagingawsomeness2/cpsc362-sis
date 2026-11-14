// Registrar (admin) portal: accounts, the course catalog, class schedules, majors,
// transcripts and graduation checks.

import { api, badge, blankToNull, codeList, el, formatDate, formatRole, handleForm, notify, table } from './api.js';
import { run, startPortal } from './portal.js';
import { fact, seats, today, transcriptDocument } from './records-view.js';

const me = startPortal('ADMIN', {
  accounts: showAccounts,
  courses: showCourses,
  schedule: showSchedule,
  majors: showMajors,
  records: () => {},
});

/* Accounts */

const accountFilter = document.getElementById('account-filter');
let accounts = [];
accountFilter.addEventListener('input', renderAccounts);

async function showAccounts() {
  const [list, majors] = await Promise.all([api('/api/admin/accounts'), api('/api/admin/majors')]);
  accounts = list;
  fillSelect(document.querySelector('#add-student [name="majorCode"]'),
    [['', 'Undeclared'], ...majors.map((major) => [major.code, `${major.name} (${major.degree})`])]);
  renderAccounts();
}

function renderAccounts() {
  const wanted = accountFilter.value.trim().toLowerCase();
  const shown = accounts.filter((account) => !wanted
    || [account.displayName, account.username, account.cwid, account.department]
      .some((value) => value && value.toLowerCase().includes(wanted)));
  document.getElementById('account-list').replaceChildren(shown.length
    ? table(['Name', 'Username', 'Role', 'CWID or department', 'Access', ''], shown.map((account) => [
      account.displayName,
      account.username,
      formatRole(account.role),
      account.cwid ?? account.department ?? '',
      account.enabled ? badge('Active', 'ok') : badge('Revoked', 'bad'),
      accountActions(account),
    ]))
    : el('p', { class: 'empty' }, `No accounts match “${accountFilter.value.trim()}”.`));
}

function accountActions(account) {
  const accessButton = account.accountId === me?.accountId ? null : account.enabled
    ? el('button', { class: 'button small danger', type: 'button', onclick: () => run(() => revoke(account)) }, 'Revoke access')
    : el('button', { class: 'button small secondary', type: 'button', onclick: () => run(() => restore(account)) }, 'Restore access');
  return el('div', { class: 'row-actions' }, accessButton,
    el('button', { class: 'button small secondary', type: 'button', onclick: () => askForPassword(account) }, 'Set password'));
}

async function revoke(account) {
  if (!confirm(`Revoke access for ${account.displayName}? They are signed out and cannot sign in. Their records are kept.`)) return;
  await api(`/api/admin/accounts/${account.accountId}/revoke`, { method: 'POST' });
  notify(`Revoked access for ${account.displayName}.`);
  await showAccounts();
}

async function restore(account) {
  await api(`/api/admin/accounts/${account.accountId}/restore`, { method: 'POST' });
  notify(`Restored access for ${account.displayName}.`);
  await showAccounts();
}

const passwordDialog = document.getElementById('password-dialog');
const passwordForm = passwordDialog.querySelector('form');
let passwordFor;

function askForPassword(account) {
  passwordFor = account;
  passwordDialog.querySelector('[data-account-name]').textContent = account.displayName;
  passwordForm.reset();
  passwordDialog.showModal();
}

handleForm(passwordForm, async ({ password }) => {
  await api(`/api/admin/accounts/${passwordFor.accountId}/password`, { method: 'PUT', body: { password } });
  passwordDialog.close();
  notify(`Set a new password for ${passwordFor.displayName}.`);
});
passwordDialog.querySelector('[data-cancel]').addEventListener('click', () => passwordDialog.close());

handleForm(document.getElementById('add-student'), async (fields, event) => {
  const account = await api('/api/admin/students', {
    method: 'POST',
    body: {
      cwid: fields.cwid.trim(),
      name: fields.name.trim(),
      email: blankToNull(fields.email),
      majorCode: blankToNull(fields.majorCode),
      username: fields.username.trim(),
      password: fields.password,
    },
  });
  event.target.reset();
  notify(`Added ${account.displayName}. They can sign in as ${account.username}.`);
  await showAccounts();
});

handleForm(document.getElementById('add-professor'), async (fields, event) => {
  const account = await api('/api/admin/professors', {
    method: 'POST',
    body: {
      name: fields.name.trim(),
      department: fields.department.trim(),
      email: blankToNull(fields.email),
      username: fields.username.trim(),
      password: fields.password,
    },
  });
  event.target.reset();
  notify(`Added ${account.displayName}. They can sign in as ${account.username}.`);
  await showAccounts();
});

/* Course catalog */

async function showCourses() {
  const courses = await api('/api/catalog/courses');
  document.getElementById('course-list').replaceChildren(table(['Code', 'Title', 'Units', 'Prerequisites'],
    courses.map((course) => [
      el('span', { class: 'course-code' }, course.code),
      course.title,
      course.units,
      course.prerequisites.length ? course.prerequisites.join(', ') : el('span', { class: 'muted' }, 'None'),
    ])));
}

handleForm(document.getElementById('add-course'), async (fields, event) => {
  const course = await api('/api/admin/courses', {
    method: 'POST',
    body: {
      code: fields.code,
      title: fields.title.trim(),
      units: Number(fields.units),
      description: blankToNull(fields.description),
      prerequisites: codeList(fields.prerequisites),
    },
  });
  event.target.reset();
  notify(`Added ${course.code} ${course.title} to the catalog.`);
  await showCourses();
});

/* Terms and sections */

const scheduleTerm = document.getElementById('schedule-term');
const sectionForm = document.getElementById('add-section');
scheduleTerm.addEventListener('change', () => run(showSections));

async function showSchedule() {
  const [terms, courses, professors] = await Promise.all([
    api('/api/catalog/terms'), api('/api/catalog/courses'), api('/api/admin/professors'),
  ]);
  document.getElementById('term-list').replaceChildren(table(['Term', 'Classes', 'Last day to add or drop', 'Registration'],
    [...terms].reverse().map((term) => [
      term.name,
      `${formatDate(term.startDate)} to ${formatDate(term.endDate)}`,
      formatDate(term.addDropDeadline),
      term.addDropOpen ? badge('Open', 'ok') : badge('Closed', 'neutral'),
    ])));

  const termOptions = [...terms].reverse().map((term) => [term.id, term.name]);
  const preferred = terms.find((term) => term.addDropOpen) ?? terms[terms.length - 1];
  fillSelect(scheduleTerm, termOptions, preferred?.id);
  fillSelect(sectionForm.elements.namedItem('termId'), termOptions, scheduleTerm.value);
  fillSelect(sectionForm.elements.namedItem('courseCode'), courses.map((course) => [course.code, `${course.code} ${course.title}`]));
  fillSelect(sectionForm.elements.namedItem('professorId'),
    [['', 'Staff (not assigned yet)'], ...professors.map((professor) => [professor.id, `${professor.name}, ${professor.department}`])]);
  await showSections();
}

async function showSections() {
  if (!scheduleTerm.value) return;
  const sections = await api(`/api/catalog/terms/${scheduleTerm.value}/sections`);
  document.getElementById('section-list').replaceChildren(sections.length
    ? table(['Course', 'Section', 'Meets', 'Room', 'Instructor', 'Seats'], sections.map((section) => [
      el('span', {}, el('span', { class: 'course-code' }, section.courseCode), ' ', section.courseTitle),
      section.sectionNumber,
      el('span', { class: 'nowrap' }, section.meets),
      el('span', { class: 'nowrap' }, section.room ?? ''),
      el('span', { class: 'nowrap' }, section.professorName),
      seats(section),
    ]))
    : el('p', { class: 'empty' }, 'No sections are scheduled for this term yet.'));
}

handleForm(document.getElementById('add-term'), async (fields, event) => {
  const term = await api('/api/admin/terms', {
    method: 'POST',
    body: { name: fields.name.trim(), startDate: fields.startDate, endDate: fields.endDate },
  });
  event.target.reset();
  notify(`Added ${term.name}. Add and drop close on ${formatDate(term.addDropDeadline)}.`);
  await showSchedule();
});

handleForm(sectionForm, async (fields) => {
  const section = await api('/api/admin/sections', {
    method: 'POST',
    body: {
      termId: Number(fields.termId),
      courseCode: fields.courseCode,
      sectionNumber: fields.sectionNumber.trim(),
      professorId: fields.professorId ? Number(fields.professorId) : null,
      capacity: Number(fields.capacity),
      days: blankToNull(fields.days),
      startTime: blankToNull(fields.startTime),
      endTime: blankToNull(fields.endTime),
      room: blankToNull(fields.room),
    },
  });
  notify(`Added ${section.courseCode} section ${section.sectionNumber} to ${section.termName}.`);
  scheduleTerm.value = String(section.termId);
  await showSections();
});

/* Majors */

const majorForm = document.getElementById('define-major');

async function showMajors() {
  const majors = await api('/api/admin/majors');
  document.getElementById('major-list').replaceChildren(majors.length
    ? table(['Code', 'Major', 'Degree', 'Units', 'Required courses', ''], majors.map((major) => [
      el('span', { class: 'course-code' }, major.code),
      major.name,
      major.degree,
      major.totalUnits,
      major.requiredCourses.join(', '),
      el('button', { class: 'button small secondary', type: 'button', onclick: () => editMajor(major) }, 'Edit'),
    ]))
    : el('p', { class: 'empty' }, 'No majors yet. Define the first one above.'));
}

function editMajor(major) {
  const field = (name) => majorForm.elements.namedItem(name);
  field('code').value = major.code;
  field('name').value = major.name;
  field('degree').value = major.degree;
  field('totalUnits').value = major.totalUnits;
  field('requiredCourses').value = major.requiredCourses.join(', ');
  majorForm.closest('details').open = true;
  field('requiredCourses').focus();
}

handleForm(majorForm, async (fields) => {
  const major = await api(`/api/admin/majors/${encodeURIComponent(fields.code.trim())}`, {
    method: 'PUT',
    body: {
      name: fields.name.trim(),
      degree: fields.degree.trim(),
      totalUnits: Number(fields.totalUnits),
      requiredCourses: codeList(fields.requiredCourses),
    },
  });
  notify(`Saved the requirements for ${major.name} (${major.degree}).`);
  await showMajors();
});

/* Transcripts and graduation checks */

handleForm(document.getElementById('records-form'), async (fields, event) => {
  const cwid = encodeURIComponent(fields.cwid.trim());
  const result = document.getElementById('records-result');
  if (event.submitter?.value === 'graduation') {
    result.replaceChildren(graduationReport(await api(`/api/admin/students/${cwid}/graduation-check`)));
  } else {
    const transcript = await api(`/api/admin/students/${cwid}/transcript`);
    result.replaceChildren(el('div', { class: 'panel' },
      el('div', { class: 'form-actions no-print' },
        el('button', { class: 'button secondary', type: 'button', onclick: () => window.print() }, 'Print transcript')),
      transcriptDocument(transcript, today())));
  }
});

function graduationReport(check) {
  const progress = el('span');
  progress.style.width = `${Math.min(100, Math.round((check.unitsCompleted / check.unitsRequired) * 100))}%`;
  return el('div', { class: 'panel' },
    el('h2', {}, `Graduation check for ${check.student.name}`),
    el('div', { class: 'verdict' },
      check.eligible ? badge('Eligible to graduate', 'ok') : badge('Not yet eligible', 'warn'),
      el('span', { class: 'muted' }, `${check.majorName} (${check.degree}), checked ${formatDate(check.checkedOn)}`)),
    el('dl', { class: 'facts' },
      fact('CWID', check.student.cwid),
      fact('Units completed', `${check.unitsCompleted} of ${check.unitsRequired}`),
      fact('Units still needed', check.unitsRemaining)),
    el('div', { class: 'progress', 'aria-hidden': 'true' }, progress),
    el('div', { class: 'split' },
      el('div', {},
        el('h3', {}, `Required courses still needed (${check.missingCourses.length})`),
        check.missingCourses.length
          ? el('ul', {}, check.missingCourses.map((code) => el('li', {}, code)))
          : el('p', { class: 'muted' }, 'Every required course is complete.')),
      el('div', {},
        el('h3', {}, `Required courses completed (${check.completedCourses.length})`),
        el('ul', {}, check.completedCourses.map((code) => el('li', {}, code))))));
}

/** Replaces a select's options, keeping (or choosing) the selected value when it is still offered. */
function fillSelect(select, options, preferred) {
  const wanted = String(preferred ?? select.value);
  select.replaceChildren(...options.map(([value, label]) => el('option', { value }, label)));
  if (options.some(([value]) => String(value) === wanted)) select.value = wanted;
}
