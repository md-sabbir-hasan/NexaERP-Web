export interface JwtPayload {
  sub: string;
  userId: number;
  permissions: string[];
  iat: number;
  exp: number;
}
