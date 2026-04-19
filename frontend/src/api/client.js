// Thin fetch wrapper. Always sends cookies so the Spring Security
// session cookie flows with each request.

const defaults = {
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
};

async function handle(res) {
  if (res.status === 204) return null;
  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const err = new Error(body?.message || res.statusText);
    err.status = res.status;
    err.body = body;
    throw err;
  }
  return body;
}

export function apiGet(path) {
  return fetch(path, { ...defaults, method: 'GET' }).then(handle);
}

export function apiPost(path, data) {
  return fetch(path, {
    ...defaults,
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  }).then(handle);
}

export function apiPut(path, data) {
  return fetch(path, {
    ...defaults,
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
  }).then(handle);
}

export function apiDelete(path) {
  return fetch(path, { ...defaults, method: 'DELETE' }).then(handle);
}
