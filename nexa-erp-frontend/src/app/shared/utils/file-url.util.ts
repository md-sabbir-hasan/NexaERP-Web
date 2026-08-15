import { APP_CONFIG } from '../../core/config/app.config';

// Backend returns relative URLs like "/api/files/PROFILE/1/xxx.jpg".
// APP_CONFIG.apiUrl already ends with "/api", so strip that to get the origin.
const API_ORIGIN = APP_CONFIG.apiUrl.replace(/\/api\/?$/, '');

export function resolveFileUrl(path: string | null | undefined): string | null {
  if (!path) return null;
  if (/^https?:\/\//i.test(path)) return path;
  return `${API_ORIGIN}${path}`;
}
