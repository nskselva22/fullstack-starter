import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { apiGet, apiPost } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const data = await apiGet('/api/auth/user');
      setUser(data?.authenticated ? data : null);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  const login = (provider = 'google') => {
    // Spring Security OAuth2 login endpoint.
    // After success, Spring redirects back to FRONTEND_URL.
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  const logout = async () => {
    try {
      await apiPost('/api/auth/logout');
    } finally {
      setUser(null);
      window.location.href = '/login';
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
