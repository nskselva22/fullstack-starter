import { useAuth } from '../context/AuthContext.jsx';
import { Navigate } from 'react-router-dom';

export default function Login() {
  const { user, loading, login } = useAuth();
  if (loading) return <div className="center">Loading…</div>;
  if (user)    return <Navigate to="/" replace />;

  return (
    <div className="login-card">
      <h1>Welcome</h1>
      <p>Sign in to continue to your tasks.</p>
      <button className="btn btn-primary" onClick={() => login('google')}>
        Sign in with Google
      </button>
      {/* Enable after adding GitHub in application.yml + Google Cloud Console:
      <button className="btn" onClick={() => login('github')}>
        Sign in with GitHub
      </button> */}
    </div>
  );
}
