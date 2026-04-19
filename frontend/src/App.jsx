import { Routes, Route, Navigate, Link } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import Login from './components/Login.jsx';
import TaskList from './components/TaskList.jsx';

function Protected({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="center">Loading…</div>;
  if (!user)   return <Navigate to="/login" replace />;
  return children;
}

function Header() {
  const { user, logout } = useAuth();
  return (
    <header className="header">
      <Link to="/" className="brand">Fullstack Starter</Link>
      <nav>
        {user ? (
          <>
            {user.picture && (
              <img src={user.picture} alt="" className="avatar" />
            )}
            <span className="user-name">{user.name || user.email}</span>
            <button onClick={logout} className="btn-link">Sign out</button>
          </>
        ) : (
          <Link to="/login" className="btn-link">Sign in</Link>
        )}
      </nav>
    </header>
  );
}

export default function App() {
  return (
    <div className="app">
      <Header />
      <main className="main">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={
            <Protected><TaskList /></Protected>
          } />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
