export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // backend sends expiry in ms
  userId: number;
  name: string;
  email: string;
}

export interface CurrentUserProfile {
  id: number;
  name: string;
  email: string;
  status: string;
  roles: string[];
  permissions: string[];
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

export interface SetPasswordRequest {
  inviteToken: string;
  password: string;
  confirmPassword: string;
}
