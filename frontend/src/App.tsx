import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import EventosPage from './pages/aluno/EventosPage';
import MinhasInscricoesPage from './pages/aluno/MinhasInscricoesPage';
import HistoricoPage from './pages/aluno/HistoricoPage';
import CertificadosPage from './pages/aluno/CertificadosPage';
import PontuacaoPage from './pages/aluno/PontuacaoPage';
import PerfilPage from './pages/aluno/PerfilPage';
import AdminEventosPage from './pages/admin/AdminEventosPage';
import AdminUsuariosPage from './pages/admin/AdminUsuariosPage';
import AdminCheckinPage from './pages/admin/AdminCheckinPage';
import type { Perfil } from './types';

function RotaPrivada({ perfis, children }: { perfis?: Perfil[]; children: ReactNode }) {
  return (
    <ProtectedRoute perfis={perfis}>
      <Navbar />
      <main>{children}</main>
    </ProtectedRoute>
  );
}

function RotaInicial() {
  const { autenticado, perfil } = useAuth();
  if (!autenticado) return <Navigate to="/login" replace />;
  return <Navigate to={perfil === 'ALUNO' ? '/eventos' : '/admin/eventos'} replace />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RotaInicial />} />

      {/* Aluno */}
      <Route
        path="/eventos"
        element={
          <RotaPrivada perfis={['ALUNO']}>
            <EventosPage />
          </RotaPrivada>
        }
      />
      <Route
        path="/minhas-inscricoes"
        element={
          <RotaPrivada perfis={['ALUNO']}>
            <MinhasInscricoesPage />
          </RotaPrivada>
        }
      />
      <Route
        path="/historico"
        element={
          <RotaPrivada perfis={['ALUNO']}>
            <HistoricoPage />
          </RotaPrivada>
        }
      />
      <Route
        path="/certificados"
        element={
          <RotaPrivada perfis={['ALUNO']}>
            <CertificadosPage />
          </RotaPrivada>
        }
      />
      <Route
        path="/pontuacao"
        element={
          <RotaPrivada perfis={['ALUNO']}>
            <PontuacaoPage />
          </RotaPrivada>
        }
      />
      <Route
        path="/perfil"
        element={
          <RotaPrivada>
            <PerfilPage />
          </RotaPrivada>
        }
      />

      {/* Admin / Professor */}
      <Route
        path="/admin/eventos"
        element={
          <RotaPrivada perfis={['ADMIN', 'PROFESSOR']}>
            <AdminEventosPage />
          </RotaPrivada>
        }
      />
      <Route
        path="/admin/usuarios"
        element={
          <RotaPrivada perfis={['ADMIN', 'PROFESSOR']}>
            <AdminUsuariosPage />
          </RotaPrivada>
        }
      />
      <Route
        path="/admin/checkin"
        element={
          <RotaPrivada perfis={['ADMIN', 'PROFESSOR']}>
            <AdminCheckinPage />
          </RotaPrivada>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}
