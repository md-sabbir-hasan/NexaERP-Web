export type UserStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'LOCKED';

export interface User {
  id: number;
  name: string;
  email: string;
  status: UserStatus;
  failedLoginAttempts: number;
  lastLoginAt: string | null;
  createdAt: string;
  roles: string[];
  permissions: string[];
}

export interface UserRequest {
  name: string;
  email: string;
  roleIds: number[];
}