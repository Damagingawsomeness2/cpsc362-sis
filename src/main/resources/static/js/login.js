import { api, portalFor, saveSession, session } from './api.js';

const signedIn = session();
if (signedIn) location.replace(portalFor(signedIn.role));

const form = document.getElementById('sign-in');
const error = document.getElementById('sign-in-error');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  error.hidden = true;
  const button = event.submitter;
  button.disabled = true;
  const fields = new FormData(form);
  try {
    const login = await api('/api/auth/login', {
      method: 'POST',
      body: { username: fields.get('username'), password: fields.get('password') },
    });
    saveSession(login);
    location.replace(portalFor(login.user.role));
  } catch (failure) {
    error.textContent = failure.message;
    error.hidden = false;
    button.disabled = false;
  }
});
