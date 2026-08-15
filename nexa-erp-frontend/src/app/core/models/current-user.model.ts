export interface CurrentUser {
  id: number;
  name: string;
  email: string;
  status: string;
  profileImageUrl: string | null;
  roles: string[];
  permissions: string[];
}
