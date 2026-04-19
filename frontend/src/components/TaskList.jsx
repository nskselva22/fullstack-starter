import { useEffect, useState } from 'react';
import { apiGet, apiPost, apiPut, apiDelete } from '../api/client.js';

export default function TaskList() {
  const [tasks, setTasks] = useState([]);
  const [title, setTitle] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError]   = useState(null);

  const load = async () => {
    try {
      setLoading(true);
      setTasks(await apiGet('/api/tasks'));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const add = async (e) => {
    e.preventDefault();
    if (!title.trim()) return;
    await apiPost('/api/tasks', { title, description: '', done: false });
    setTitle('');
    load();
  };

  const toggle = async (t) => {
    await apiPut(`/api/tasks/${t.id}`, { ...t, done: !t.done });
    load();
  };

  const remove = async (t) => {
    await apiDelete(`/api/tasks/${t.id}`);
    load();
  };

  if (loading) return <div className="center">Loading tasks…</div>;
  if (error)   return <div className="error">Error: {error}</div>;

  return (
    <div className="tasks">
      <h1>My Tasks</h1>

      <form onSubmit={add} className="task-form">
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="What needs doing?"
          maxLength={200}
        />
        <button className="btn btn-primary" type="submit">Add</button>
      </form>

      {tasks.length === 0 ? (
        <p className="muted">No tasks yet — add your first one above.</p>
      ) : (
        <ul className="task-list">
          {tasks.map((t) => (
            <li key={t.id} className={t.done ? 'done' : ''}>
              <label>
                <input
                  type="checkbox"
                  checked={t.done}
                  onChange={() => toggle(t)}
                />
                <span>{t.title}</span>
              </label>
              <button className="btn-link danger" onClick={() => remove(t)}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
