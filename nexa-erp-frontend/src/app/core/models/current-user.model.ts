export interface CurrentUser {
  id: number;
  name: string;
  email: string;
  status: string;
  roles: string[];
  permissions: string[];
}
