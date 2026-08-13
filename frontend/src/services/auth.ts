import api from './api';
import type { LoginRequest, LoginResponse } from '../types';

export function login(dados: LoginRequest) {
  return api.post<LoginResponse>('/auth/login', dados).then((res) => res.data);
}
